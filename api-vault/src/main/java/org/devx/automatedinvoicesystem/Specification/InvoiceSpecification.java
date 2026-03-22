package org.devx.automatedinvoicesystem.Specification;

import jakarta.persistence.criteria.Predicate;
import org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InvoiceSpecification {

    public static Specification<Invoice> getSearchSpecification(InvoiceSearchFilter filter) {
        return (root, query, criteriaBuilder) -> {

            // This list holds all our dynamic "AND" clauses
            List<Predicate> predicates = new ArrayList<>();

            // "WHERE organization_id = ?"
            predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), filter.getOrganizationId()));

            // DYNAMIC FILTERS (Only added if the user requested them)

            if (filter.getVendorName() != null && !filter.getVendorName().trim().isEmpty()) {
                // "AND vendor_name ILIKE '%amazon%'" (Case-insensitive search)
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("vendorName")),
                        "%" + filter.getVendorName().toLowerCase() + "%"
                ));
            }

            if (filter.getCategory() != null && !filter.getCategory().trim().isEmpty()) {
                // "AND category = 'SOFTWARE'"
                predicates.add(criteriaBuilder.equal(root.get("category"), filter.getCategory()));
            }

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                // "AND status = 'COMPLETED'"
                predicates.add(criteriaBuilder.equal(root.get("status"), Invoice.ProcessingStatus.valueOf(filter.getStatus())));
            }

            // THE DATE RANGE
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                // "AND invoice_date BETWEEN startDate AND endDate"
                predicates.add(criteriaBuilder.between(root.get("invoiceDate"), filter.getStartDate(), filter.getEndDate()));
            } else if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("invoiceDate"), filter.getStartDate()));
            } else if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("invoiceDate"), filter.getEndDate()));
            }

            // Glue all the predicates together with "AND"
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}