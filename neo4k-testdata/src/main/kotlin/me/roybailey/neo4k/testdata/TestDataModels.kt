package me.roybailey.neo4k.testdata

import java.time.LocalDate


data class Supplier(
        val supplierId:String,
        val name:String,
        val discount: Int,
        var products: List<Product> = emptyList()
)

enum class Category {
    FOOD, TOYS, BOOKS, DIY
}

data class Product(
        val productId:String,
        val supplierId:String,
        val name:String,
        val category: Category,
        val units: Int,
        val clearance: Boolean,
        val bestBefore: LocalDate
)

object DataModel {

    val supplierHeaders = arrayOf(
            "SupplierId",
            "Name",
            "Discount"
    )

    fun supplierValues(supplier: Supplier) = arrayOf(
            supplier.supplierId,
            supplier.name,
            supplier.discount
    )

    val productHeaders = arrayOf(
            "ProductId",
            "SupplierId",
            "Name",
            "Category",
            "Units",
            "Clearance",
            "BestBefore"
    )

    fun productValues(product: Product) = arrayOf(
            product.productId,
            product.supplierId,
            product.name,
            product.category,
            product.units,
            product.clearance,
            product.bestBefore
    )

}