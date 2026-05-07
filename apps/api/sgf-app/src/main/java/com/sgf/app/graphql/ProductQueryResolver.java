package com.sgf.app.graphql;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProductQueryResolver {

    private final ProductRepository productRepository;

    public ProductQueryResolver(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @QueryMapping
    public List<Product> products(@Argument Integer limit) {
        // Implementation detail: for now we just return all products
        return productRepository.findAll();
    }

    @QueryMapping
    public Optional<Product> product(@Argument String id) {
        return productRepository.findById(UUID.fromString(id));
    }
}
