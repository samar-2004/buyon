package com.buyon.domain.repository;

import com.buyon.domain.callback.DomainCallback;
import com.buyon.domain.callback.RepositoryListener;
import com.buyon.domain.model.Category;

import java.util.List;

public interface CategoryRepository {
    void startCategoriesListener(RepositoryListener<List<Category>> listener);

    void createCategory(Category category, DomainCallback<String> callback);

    void stopCategoriesListener();
}
