// A standalone, parametrized model — no CarListing/carstone coupling, deliberately structured
// the same way sidebar-crudstone/searchcrudstone's own config models are, so this component could
// be lifted into its own "footer-crudstone" library later without changes, the same family
// pattern @Sidebar/@Searchable already established for this ecosystem.
export interface FooterLink {
  label: string;
  path: string;
}

export interface FooterColumn {
  title: string;
  links: FooterLink[];
}

export interface FooterSocialLink {
  label: string;
  href: string;
  icon: string; // a PrimeIcons class name, e.g. "pi-facebook"
}

export interface FooterConfig {
  brand: string;
  tagline: string;
  columns: FooterColumn[];
  socialLinks: FooterSocialLink[];
  copyright: string;
}
