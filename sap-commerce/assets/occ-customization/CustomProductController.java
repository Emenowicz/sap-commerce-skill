/*
 * CustomProductController.java
 * REST controller for custom product endpoints.
 * Demonstrates OCC API patterns with Swagger documentation.
 */
package com.example.controllers;

import com.example.dto.CustomProductWsDTO;
import com.example.dto.CustomProductListWsDTO;
import com.example.facades.CustomProductFacade;
import com.example.facades.data.CustomProductData;

import de.hybris.platform.commercewebservicescommons.dto.product.ProductWsDTO;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * REST controller for custom product operations.
 *
 * URL Pattern: /occ/v2/{baseSiteId}/customproducts
 */
@Controller
@RequestMapping("/{baseSiteId}/customproducts")
@Api(tags = "Custom Products")
public class CustomProductController {

    @Resource
    private CustomProductFacade customProductFacade;

    @Resource
    private DataMapper dataMapper;

    /**
     * GET /customproducts
     * Retrieve list of custom products with optional filtering.
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
        value = "Get custom products",
        notes = "Returns a list of custom products with pagination support"
    )
    @ApiBaseSiteIdParam
    public CustomProductListWsDTO getCustomProducts(
            @ApiParam(value = "Base site identifier", required = true)
            @PathVariable String baseSiteId,

            @ApiParam(value = "Search query")
            @RequestParam(required = false) String query,

            @ApiParam(value = "Current page number", defaultValue = "0")
            @RequestParam(defaultValue = "0") int currentPage,

            @ApiParam(value = "Page size", defaultValue = "20")
            @RequestParam(defaultValue = "20") int pageSize,

            @ApiParam(value = "Response field level", defaultValue = "DEFAULT")
            @RequestParam(defaultValue = "DEFAULT") String fields) {

        List<CustomProductData> products = customProductFacade.searchProducts(query, currentPage, pageSize);

        CustomProductListWsDTO result = new CustomProductListWsDTO();
        result.setProducts(dataMapper.mapAsList(products, CustomProductWsDTO.class, fields));
        result.setTotalCount(customProductFacade.getTotalCount(query));
        return result;
    }

    /**
     * GET /customproducts/{productCode}
     * Retrieve single custom product by code.
     */
    @RequestMapping(value = "/{productCode}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
        value = "Get custom product by code",
        notes = "Returns detailed information about a specific custom product"
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "Product found"),
        @ApiResponse(code = 404, message = "Product not found")
    })
    public CustomProductWsDTO getCustomProduct(
            @ApiParam(value = "Base site identifier", required = true)
            @PathVariable String baseSiteId,

            @ApiParam(value = "Product code", required = true)
            @PathVariable String productCode,

            @ApiParam(value = "Response field level", defaultValue = "DEFAULT")
            @RequestParam(defaultValue = "DEFAULT") String fields) {

        CustomProductData productData = customProductFacade.getProductForCode(productCode);
        return dataMapper.map(productData, CustomProductWsDTO.class, fields);
    }

    /**
     * POST /customproducts
     * Create a new custom product.
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @ApiOperation(
        value = "Create custom product",
        notes = "Creates a new custom product and returns the created resource"
    )
    @ApiResponses({
        @ApiResponse(code = 201, message = "Product created successfully"),
        @ApiResponse(code = 400, message = "Invalid request data")
    })
    public CustomProductWsDTO createCustomProduct(
            @ApiParam(value = "Base site identifier", required = true)
            @PathVariable String baseSiteId,

            @ApiParam(value = "Product data", required = true)
            @RequestBody CustomProductWsDTO productDto) {

        CustomProductData productData = dataMapper.map(productDto, CustomProductData.class);
        CustomProductData createdProduct = customProductFacade.createProduct(productData);
        return dataMapper.map(createdProduct, CustomProductWsDTO.class, "FULL");
    }

    /**
     * PUT /customproducts/{productCode}
     * Update existing custom product.
     */
    @RequestMapping(value = "/{productCode}", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(
        value = "Update custom product",
        notes = "Updates an existing custom product"
    )
    public CustomProductWsDTO updateCustomProduct(
            @PathVariable String baseSiteId,
            @PathVariable String productCode,
            @RequestBody CustomProductWsDTO productDto) {

        CustomProductData productData = dataMapper.map(productDto, CustomProductData.class);
        productData.setCode(productCode);
        CustomProductData updatedProduct = customProductFacade.updateProduct(productData);
        return dataMapper.map(updatedProduct, CustomProductWsDTO.class, "FULL");
    }

    /**
     * DELETE /customproducts/{productCode}
     * Delete a custom product.
     */
    @RequestMapping(value = "/{productCode}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Delete custom product")
    public void deleteCustomProduct(
            @PathVariable String baseSiteId,
            @PathVariable String productCode) {

        customProductFacade.deleteProduct(productCode);
    }

    @ExceptionHandler(UnknownIdentifierException.class)
    @ResponseBody
    public ResponseEntity<String> handleUnknownIdentifier(final UnknownIdentifierException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> handleIllegalArgument(final IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // Setter for testing
    public void setCustomProductFacade(CustomProductFacade customProductFacade) {
        this.customProductFacade = customProductFacade;
    }

    public void setDataMapper(DataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }
}
