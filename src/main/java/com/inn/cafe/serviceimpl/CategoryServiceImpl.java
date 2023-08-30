package com.inn.cafe.serviceimpl;

import com.google.common.base.Strings;
import com.inn.cafe.JWT.JwtAuthFilter;
import com.inn.cafe.POJO.Category;
import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.dao.CategoryDao;
import com.inn.cafe.service.CategoryService;
import com.inn.cafe.utils.CafeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDao categoryDao;
    private final JwtAuthFilter jwtAuthFilter;

    @Override
    public ResponseEntity<String> addNewCategory(Map<String, String> requestMap) {
        try {
            if (jwtAuthFilter.isAdmin()){
                if (validaCategoryMap(requestMap, false)){
                    categoryDao.save(getCategoryFromMap(requestMap, false));
                    return CafeUtils.getResponseEntity("Category added successfully", HttpStatus.OK);
                }
            }
            return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<Category>> getAllCategory(String filterValue) {
        try {
            if (!Strings.isNullOrEmpty(filterValue) && filterValue.equalsIgnoreCase("true")){
                return new ResponseEntity<>(categoryDao.getAllCategory(), HttpStatus.OK);
            }
            return new ResponseEntity<>(categoryDao.findAll(), HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateCategory(Map<String, String> requestMap) {
        try {
            if (jwtAuthFilter.isAdmin()){
                if (validaCategoryMap(requestMap, true)){
                    Optional<Category> optional = categoryDao.findById(Integer.parseInt(requestMap.get("id")));
                    if (optional.isPresent()){
                        categoryDao.save(getCategoryFromMap(requestMap, true));
                        return CafeUtils.getResponseEntity("Category updated successfully", HttpStatus.OK);
                    }
                    return CafeUtils.getResponseEntity("Category id doesn't exist", HttpStatus.OK);
                }
                return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
            return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validaCategoryMap(Map<String, String> requestMap, boolean validateId) {
        if (requestMap.containsKey("name")){
            if (requestMap.containsKey("id") && validateId){
                return true;
            }else return !validateId;
        }
        return false;
    }

    private Category getCategoryFromMap(Map<String, String> requestMap, Boolean isAdd){
        Category category = new Category();
        if (isAdd) {
            category.setId(Integer.parseInt(requestMap.get("id")));
        }
        category.setName(requestMap.get("name"));
        return category;
    }
}
