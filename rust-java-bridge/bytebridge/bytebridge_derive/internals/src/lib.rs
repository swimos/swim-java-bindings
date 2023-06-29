use proc_macro2::{Ident, TokenStream};
use quote::__private::ext::RepToTokensExt;
use quote::{format_ident, quote, ToTokens};
use syn::{parse2, Attribute, Data, DeriveInput, Error, Fields, Meta, Type, Visibility};

pub fn derive(input: TokenStream) -> TokenStream {
    let input = match parse2::<DeriveInput>(input) {
        Ok(input) => input,
        Err(e) => return e.to_compile_error(),
    };
    expand(input).unwrap_or_else(|e| e.to_compile_error())
}

fn expand(input: DeriveInput) -> Result<TokenStream, Error> {
    let ty_ident = &input.ident;
    let repr = derive_byte_transformations(&input)?;

    let try_from_bytes = TryFromBytes(&repr);
    let to_bytes = ToBytes(&repr);
    let ident = format_ident!("_");

    let tokens = quote! {
        const #ident: () = {
            impl bytebridge::ByteCodec for #ty_ident {
                fn try_from_bytes(bytes: &mut bytebridge::BytesMut) -> Result<Self, bytebridge::FromBytesError>
                where
                    Self: Sized
                {
                    #try_from_bytes
                }

                fn to_bytes(&self, bytes: &mut bytebridge::BytesMut) {
                    #to_bytes
                }
            }
        };
    };

    Ok(tokens)
}

fn derive_byte_transformations(item: &DeriveInput) -> Result<ByteRepr<'_>, Error> {
    match item.generics.params.next() {
        Some(item) if item.is_empty() => {}
        _ => {
            return Err(Error::new_spanned(
                item,
                "ByteBridge does not support generic type parameters, lifetimes or const generic parameters",
            ));
        }
    }

    match &item.data {
        Data::Struct(data) => Ok(ByteRepr::Struct(StructRepr {
            ident: &item.ident,
            fields: derive_fields(&data.fields, |vis| match vis {
                Visibility::Public(_) => Ok(()),
                v @ Visibility::Restricted(_) | v @ Visibility::Inherited => {
                    Err(Error::new_spanned(v, "Field must be public"))
                }
            })?,
        })),
        Data::Enum(data) => {
            let lim = u8::MAX as usize;
            if data.variants.len() > lim {
                return Err(Error::new_spanned(
                    &data.variants,
                    format!("Enum variant limit is {}", lim),
                ));
            }
            Ok(ByteRepr::Enum(EnumRepr {
                ident: &item.ident,
                variants: derive_variants(data.variants.iter())?,
            }))
        }
        Data::Union(_) => Err(Error::new_spanned(
            item,
            "ByteBridge does not support unions",
        )),
    }
}

struct StructRepr<'a> {
    ident: &'a Ident,
    fields: Vec<Field<'a>>,
}

struct EnumRepr<'a> {
    ident: &'a Ident,
    variants: Vec<Variant<'a>>,
}

struct Field<'a> {
    ty: &'a Type,
    ident: &'a Ident,
    cfg_attrs: Vec<&'a Attribute>,
}

struct Variant<'a> {
    ident: &'a Ident,
    fields: Vec<Field<'a>>,
}

enum ByteRepr<'a> {
    Struct(StructRepr<'a>),
    Enum(EnumRepr<'a>),
}

struct TryFromBytes<'v, 'a>(&'v ByteRepr<'a>);
impl<'v, 'a> TryFromBytes<'v, 'v> {
    fn fold_fields(fields: &[Field<'a>]) -> TokenStream {
        fields.iter().fold(TokenStream::new(), |tokens, field| {
            let Field {
                ty,
                ident,
                cfg_attrs,
            } = field;
            let attrs = cfg_attrs.iter().fold(TokenStream::new(), |ts, attr| {
                quote! {
                    #ts
                    #attr
                }
            });
            quote! {
                #tokens
                #attrs
                #ident: <#ty as bytebridge::ByteCodec>::try_from_bytes(bytes)?,
            }
        })
    }
}

impl<'v, 'a> ToTokens for TryFromBytes<'v, 'a> {
    fn to_tokens(&self, tokens: &mut TokenStream) {
        let TryFromBytes(inner) = self;
        match inner {
            ByteRepr::Struct(repr) => {
                let StructRepr { ident, fields } = repr;
                let fields = TryFromBytes::fold_fields(fields);
                let try_from_bytes = quote! {
                    Ok(#ident {
                        #fields
                    })
                };
                try_from_bytes.to_tokens(tokens);
            }
            ByteRepr::Enum(repr) => {
                let EnumRepr {
                    ident: enum_ident,
                    variants,
                } = repr;

                let arms = variants.iter().enumerate().fold(
                    TokenStream::new(),
                    |tokens, (idx, variant)| {
                        // this is safe as we've previously checked the number of variants
                        let idx = u8::try_from(idx).expect("Too many variants");
                        let Variant {
                            ident: variant_ident,
                            fields,
                        } = variant;
                        let fields = TryFromBytes::fold_fields(fields);
                        quote! {
                            #tokens
                            #idx => {
                                Ok(#enum_ident::#variant_ident {
                                    #fields
                                })
                            }
                        }
                    },
                );

                let try_from_bytes = quote! {
                    bytebridge::has_size_of::<u8>(&bytes)?;

                    match <u8 as bytebridge::ByteCodec>::try_from_bytes(bytes)? {
                        #arms
                        n => Err(bytebridge::FromBytesError::UnknownEnumVariant(n)),
                    }
                };
                try_from_bytes.to_tokens(tokens);
            }
        }
    }
}

struct ToBytes<'v, 'a>(&'v ByteRepr<'a>);
impl<'v, 'a> ToBytes<'v, 'a> {
    fn destruct(ident: &Ident, fields: &[Field<'a>]) -> TokenStream {
        let fields = fields.iter().fold(TokenStream::new(), |tokens, field| {
            let field_ident = field.ident;
            let attrs = field.cfg_attrs.iter().fold(TokenStream::new(), |ts, attr| {
                quote! {
                    #ts
                    #attr
                }
            });
            quote! {
                #tokens
                #attrs
                #field_ident,
            }
        });
        quote! {
            #ident { #fields }
        }
    }

    fn fold_fields(fields: &[Field<'a>]) -> TokenStream {
        fields.iter().fold(TokenStream::new(), |tokens, field| {
            let Field {
                ident, cfg_attrs, ..
            } = field;
            let attrs = cfg_attrs.iter().fold(TokenStream::new(), |ts, attr| {
                quote! {
                    #ts
                    #attr
                }
            });
            quote! {
                #tokens
                #attrs
                bytebridge::ByteCodec::to_bytes(#ident, bytes);
            }
        })
    }
}

impl<'v, 'a> ToTokens for ToBytes<'v, 'a> {
    fn to_tokens(&self, tokens: &mut TokenStream) {
        let ToBytes(inner) = self;
        match inner {
            ByteRepr::Struct(repr) => {
                let StructRepr { ident, fields } = repr;
                let destructed = ToBytes::destruct(ident, fields);
                let fields = ToBytes::fold_fields(fields);
                let to_bytes = quote! {
                    let #destructed = self;
                    #fields
                };
                to_bytes.to_tokens(tokens);
            }
            ByteRepr::Enum(repr) => {
                let EnumRepr {
                    ident: enum_ident,
                    variants,
                } = repr;

                let arms = variants.iter().enumerate().fold(
                    TokenStream::new(),
                    |tokens, (idx, variant)| {
                        let Variant {
                            ident: variant_ident,
                            fields,
                        } = variant;
                        let destructed = ToBytes::destruct(variant_ident, fields);
                        let fields = ToBytes::fold_fields(fields);

                        // this is safe as we've previously checked the number of variants
                        let idx = u8::try_from(idx).expect("Too many variants");

                        quote! {
                            #tokens
                            #enum_ident::#destructed => {
                                bytebridge::BufMut::put_u8(bytes, #idx);
                                #fields
                            },
                        }
                    },
                );

                let to_bytes = quote! {
                    match self {
                        #arms
                    }
                };
                to_bytes.to_tokens(tokens);
            }
        }
    }
}

fn derive_variants<'a, I>(variants: I) -> Result<Vec<Variant<'a>>, Error>
where
    I: Iterator<Item = &'a syn::Variant>,
{
    variants
        .into_iter()
        .try_fold(Vec::new(), |mut variants, variant| {
            variants.push(Variant {
                ident: &variant.ident,
                fields: derive_fields(&variant.fields, |_| Ok(()))?,
            });
            Ok(variants)
        })
}

fn derive_fields<F>(fields: &Fields, vis: F) -> Result<Vec<Field<'_>>, Error>
where
    F: Fn(&Visibility) -> Result<(), Error>,
{
    match fields {
        Fields::Named(fields) => fields
            .named
            .iter()
            .try_fold(Vec::new(), |mut fields, field| {
                vis(&field.vis)?;

                fields.push(Field {
                    ty: &field.ty,
                    ident: field.ident.as_ref().expect("Named field missing ident"),
                    cfg_attrs: inspect_cfg_attrs(&field.attrs)?,
                });
                Ok(fields)
            }),
        Fields::Unnamed(_) => Err(Error::new_spanned(
            fields,
            "Bytebridge does not support tuple fields",
        )),
        Fields::Unit => Err(Error::new_spanned(
            fields,
            "Bytebridge does not support unit structs/variants",
        )),
    }
}

fn inspect_cfg_attrs(attrs: &[Attribute]) -> Result<Vec<&Attribute>, Error> {
    let mut cfg_attrs = Vec::new();

    for attr in attrs {
        match &attr.meta {
            Meta::List(inner) if inner.path.is_ident("cfg") => cfg_attrs.push(attr),
            _ => {}
        }
    }

    Ok(cfg_attrs)
}
