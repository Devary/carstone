// Mirrors context-gen's FooterContext/FooterSectionContext/... wire shape exactly (GET
// /footer/{name}) — this app no longer defines its own footer content, it renders whatever the
// backend's @Footer-annotated class declares. Kept as its own file/interfaces (not reused from
// searchcrudstone/sidebarcrudstone) since there's no shared "footer-crudstone" library yet; see
// FooterComponent's own header comment.
export interface FooterLink {
  label: string;
  url: string;
  // a PrimeIcons class (e.g. "pi pi-facebook") or an image URL — FooterComponent decides which by
  // checking whether it starts with "pi "
  logo?: string | null;
}

export interface FooterInput {
  placeholder: string;
  type: string;
  name: string;
}

export interface FooterButton {
  label: string;
  href: string;
}

export interface FooterSection {
  // a grid class, e.g. "col-md-4" — applied directly as a CSS class, see footer.component.scss's
  // own .col-md-* definitions
  style: string;
  title?: string | null;
  logo?: string | null;
  description?: string | null;
  links: FooterLink[];
  input?: FooterInput | null;
  buttons: FooterButton[];
}

export interface FooterConfig {
  name: string;
  theme: string;
  title?: string | null;
  // rendered via [innerHTML] — Angular sanitizes automatically, same as any other backend-sourced
  // HTML string already rendered that way elsewhere in this family (e.g. a textEditor field)
  copyrightHtml?: string | null;
  sections: FooterSection[];
}
