package me.roybailey.neo4k.testdata

import me.roybailey.neo4k.testdata.DataModel.productHeaders
import me.roybailey.neo4k.testdata.DataModel.productValues
import me.roybailey.neo4k.testdata.DataModel.supplierHeaders
import me.roybailey.neo4k.testdata.DataModel.supplierValues
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files


/**
 * Intended to generate controlled test data in the format of flat CSV files for testing data loading into Neo4j
 */
class GenerateCsvTest : UnitTestBase() {


    class RecordCsvWriter(filename: String, headers: Array<String>, val total: Int) {

        val writer = Files.newBufferedWriter(File(filename).toPath())
        val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(*headers))
        fun write(record: Array<Any>) = csvPrinter.printRecord(*record)
        fun print(index: Int, record: Array<Any>) {
            if (index < total)
                write(record)
        }

        fun close() = csvPrinter.close(true)
    }


    fun generate(totalSuppliers: Int, productsPerSupplier: Int) {
        val outputFolder = "$projectFolder/testdata/generator"
        val totalProducts = totalSuppliers * productsPerSupplier
        Generator().run {
            val suppliers = RecordCsvWriter("$outputFolder/suppliers-$totalSuppliers.csv", supplierHeaders, totalSuppliers)
            val products = RecordCsvWriter("$outputFolder/products-$totalProducts.csv", productHeaders, totalProducts)

            logger.info { "Generating $totalSuppliers suppliers and $productsPerSupplier products into $outputFolder" }
            generate(totalSuppliers, productsPerSupplier).forEachIndexed { supplierIndex, supplier ->
                suppliers.print(supplierIndex, supplierValues(supplier) as Array<Any>)
                supplier.products.forEachIndexed { productIndex, product ->
                    products.print(productIndex, productValues(product) as Array<Any>)
                }
            }
            suppliers.close()
            products.close()
        }
    }


    @Test
    fun `generate various sized csv files`() {

        generate(10, 10)
        generate(100, 10)
        generate(100,100)
        generate(500,100)
    }
}
