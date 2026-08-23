// Mirrors context-gen's HeaderContext/HeaderSectionContext/HeaderLinkContext wire shape exactly
// (GET /header/{name}) — this app no longer defines its own nav content, it renders whatever the
// backend's @Header-annotated class declares. See footer.model.ts's own header comment for why
// this stays local to carstone-front-ui rather than a published "header-crudstone" library.
export interface HeaderLink {
  label: string;
  type: 'link' | 'dropdown';
  // link nodes only — null for a dropdown node
  url?: string | null;
  // dropdown nodes only — empty for a link node
  children: HeaderLink[];
}

export interface HeaderSection {
  title?: string | null;
  links: HeaderLink[];
}

export interface HeaderConfig {
  name: string;
  theme: string;
  variant: 'mega_menu' | 'simple_menu';
  position: 'left' | 'right' | 'center';
  sections: HeaderSection[];
}
