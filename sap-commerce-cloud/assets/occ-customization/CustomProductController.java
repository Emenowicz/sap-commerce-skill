/*
 * CustomProductController.java
 * REST controller for custom product endpoints.
 * Demonstrates SAP Commerce 2211-jdk21 OCC patterns with OpenAPI 3 documentation.
 */
package com.example.controllers;

import com.example.dto.CustomProductWsDTO;
import com.example.dto.CustomProductListWsDTO;
import com.example.facades.CustomProductFacade;
import com.example.facades.data.CustomProductData;

import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for custom product operations.
 *
 * URL Pattern: /occ/v2/{baseSiteId}/customproducts
 */
@Controller
@RequestMapping("/{baseSiteId}/customproducts")
@Tag(name = "Custom Products")
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
    @Operation(
        summary = "Get custom products",
        description = "Returns a list of custom products with pagination support"
    )
    @ApiBaseSiteIdParam
    public CustomProductListWsDTO getCustomProducts(
            @Parameter(description = "Base site identifier", required = true)
            @PathVariable String baseSiteId,

            @Parameter(description = "Search query")
            @RequestParam(required = false) String query,

            @Parameter(description = "Current page number", example = "0")
            @RequestParam(defaultValue = "0") int currentPage,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int pageSize,

            @Parameter(description = "Response field level", example = "DEFAULT")
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
    @Operation(
        summary = "Get custom product by code",
        description = "Returns detailed information about a specific custom product"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public CustomProductWsDTO getCustomProduct(
            @Parameter(description = "Base site identifier", required = true)
            @PathVariable String baseSiteId,

            @Parameter(description = "Product code", required = true)
            @PathVariable String productCode,

            @Parameter(description = "Response field level", example = "DEFAULT")
            @RequestParam(defaultValue = "DEFAULT") String fields) {

        CustomProductData productData = customProductFacade.getProductForCode(productCode);
        return dataMapper.map(productData, CustomProductWsDTO.class, fields);
    }

    /**
     * POST /customproducts
     * Create a new custom product.
     */
    @RequestMapping(method = RequestMethod.POST)
    @Secured("ROLE_TRUSTED_CLIENT")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @Operation(
        summary = "Create custom product",
        description = "Creates a new custom product and returns the created resource"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public CustomProductWsDTO createCustomProduct(
            @Parameter(description = "Base site identifier", required = true)
            @PathVariable String baseSiteId,

            @Parameter(description = "Product data", required = true)
            @Valid @RequestBody CustomProductWsDTO productDto) {

        if (productDto.getCode() == null || productDto.getCode().isBlank()) {
            throw new IllegalArgumentException("Product code is required");
        }
        CustomProductData productData = dataMapper.map(productDto, CustomProductData.class);
        CustomProductData createdProduct = customProductFacade.createProduct(productData);
        return dataMapper.map(createdProduct, CustomProductWsDTO.class, "FULL");
    }

    /**
     * PUT /customproducts/{productCode}
     * Update existing custom product.
     */
    @RequestMapping(value = "/{productCode}", method = RequestMethod.PUT)
    @Secured("ROLE_TRUSTED_CLIENT")
    @ResponseBody
    @Operation(
        summary = "Update custom product",
        description = "Updates an existing custom product"
    )
    public CustomProductWsDTO updateCustomProduct(
            @PathVariable String baseSiteId,
            @PathVariable String productCode,
            @Valid @RequestBody CustomProductWsDTO productDto) {

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
    @Secured("ROLE_TRUSTED_CLIENT")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete custom product")
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
