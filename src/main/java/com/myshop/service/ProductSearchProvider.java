package com.myshop.service;

import com.myshop.model.Product;
import com.myshop.model.SearchRequest;

import java.util.List;

public interface ProductSearchProvider {

    List<Product> search(SearchRequest request);
}
