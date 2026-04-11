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

            List<Predicate> predicates = new ArrayList<>();

            // Mandatory: Organization scope
            predicates.add(criteriaBuilder.equal(
                    root.get("organization").get("id"), filter.getOrganizationId()));

            // Optional: Client scope
            if (filter.getClientId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("client").get("id"), filter.getClientId()));
            }

            // Dynamic filters
            if (filter.getSupplierName() != null && !filter.getSupplierName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("supplierName")),
                        "%" + filter.getSupplierName().toLowerCase() + "%"
                ));
            }

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"), Invoice.ProcessingStatus.valueOf(filter.getStatus())));
            }

            // Date range
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.between(
                        root.get("invoiceDate"), filter.getStartDate(), filter.getEndDate()));
            } else if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("invoiceDate"), filter.getStartDate()));
            } else if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("invoiceDate"), filter.getEndDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}