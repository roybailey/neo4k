package me.roybailey.neo4k.testdata

import mu.KotlinLogging
import java.lang.Math.min
import java.time.LocalDate


class Generator {

    private val logger = KotlinLogging.logger { }

    companion object {
        fun <T> newValue(data: Array<T>, index: Long, outOf: Int = Int.MAX_VALUE) = data[(index % min(data.size, outOf)).toInt()]
        fun <T> newValue(data: List<T>, index: Long, outOf: Int = Int.MAX_VALUE) = data[(index % min(data.size, outOf)).toInt()]
    }

    class SupplierGenerator {
        private var sdx = 0L
        private fun newSupplierId(): String = "SUP" + ((++sdx).toString().padStart(10, '0'))

        val supplierIds = mutableListOf<String>()

        fun newSupplier(): Supplier {
            val supplierId = newSupplierId()
            supplierIds.add(supplierId)
            return Supplier(
                    supplierId = supplierId,
                    name = "Supplier $sdx",
                    discount = newValue(arrayOf(0, 0, 0, 5, 10), sdx)
            )
        }
    }

    class ProductGenerator(val supplierGenerator: SupplierGenerator) {
        private var pdx = 0L
        private fun newProductId(): String = "PRD" + ((++pdx).toString().padStart(10, '0'))

        val productIds = mutableListOf<String>()

        fun newProduct(supplierId: String? = null): Product {
            val productId = newProductId()
            productIds.add(productId)
            return Product(
                    productId = productId,
                    supplierId = when (supplierId) {
                        null -> newValue(supplierGenerator.supplierIds, pdx - 1, supplierGenerator.supplierIds.size / 10)
                        else -> supplierId
                    },
                    name = "Product $pdx",
                    category = newValue(Category.values(), pdx),
                    units = newValue(arrayOf(10, 100, 1000), pdx),
                    clearance = newValue(arrayOf(false, true), pdx),
                    bestBefore = LocalDate.now().plusDays(newValue(arrayOf(10, 20, 30), pdx).toLong())
            )
        }
    }

    val supplierGenerator = SupplierGenerator()
    val productGenerator = ProductGenerator(supplierGenerator)

    constructor() {
        logger.info { "Generator started" }
    }

    fun generateSuppliers(total: Int, callback: (Int, Supplier) -> Unit) {
        IntRange(1, total).forEachIndexed { index, value ->
            callback(index, supplierGenerator.newSupplier())
        }
    }

    fun generateProducts(total: Int, supplierId: String? = null, callback: (Int, Product) -> Unit) {
        IntRange(1, total).forEachIndexed { index, value ->
            callback(index, productGenerator.newProduct(supplierId))
        }
    }

    fun generate(totalSuppliers: Int, totalProducts: Int): List<Supplier> {
        val listSuppliers = mutableListOf<Supplier>()
        generateSuppliers(totalSuppliers) { supplierIndex, supplier ->
            listSuppliers.add(supplier)
            generateProducts(totalProducts, supplier.supplierId) { productIndex, product ->
                supplier.products = supplier.products + product
            }
        }
        return listSuppliers.toList()
    }
}