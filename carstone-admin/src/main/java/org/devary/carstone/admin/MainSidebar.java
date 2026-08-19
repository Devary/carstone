package org.devary.carstone.admin;

import org.devary.carstone.domain.Brand;
import org.devary.carstone.domain.CarListing;
import org.devary.carstone.domain.Seller;
import org.devary.table.annotations.Sidebar;
import org.devary.table.annotations.SidebarCollapsible;
import org.devary.table.annotations.SidebarVariant;
import org.devary.table.sidebar.SidebarNode;

import java.util.List;

/**
 * Admin nav — a plain marker class, not persisted. Modeled on quar-crud-host's own
 * {@code MainSidebar}: one group holding links to every entity the admin manages.
 */
@Sidebar(name = "main", theme = "blue", variant = SidebarVariant.SIDEBAR, collapsible = SidebarCollapsible.ICON)
public class MainSidebar {

    private final List<SidebarNode> nodes = List.of(
            SidebarNode.group("pi pi-car", "Marketplace",
                    SidebarNode.link("pi pi-list", "Listings", CarListing.class),
                    SidebarNode.link("pi pi-tag", "Brands", Brand.class),
                    SidebarNode.link("pi pi-users", "Sellers", Seller.class)));
}
