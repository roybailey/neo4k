package me.roybailey.neo4k.testdata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileWriter


class GenerateJsonTest : UnitTestBase() {

    data class JsonTestData(val listSuppliers:List<Supplier>)

    private val mapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.WRAP_ROOT_VALUE, false)

    @Test
    fun `generate various sized json files`() {

        val generator = Generator()
        val outputFolder = "$projectFolder/testdata/generator"
        File(outputFolder).mkdir()

        val suppliers10 = FileWriter("$outputFolder/suppliers-10.json")
        val suppliers10list = mutableListOf<Supplier>()
        println()

        generator.generateSuppliers(total = 10) { supplierIndex, supplier ->
            println("$supplierIndex) $supplier")
            suppliers10list.add(supplier)

            generator.generateProducts(total = 100, supplierId = supplier.supplierId) { productIndex, product ->
                println("$productIndex) $product")
                supplier.products = supplier.products + product
            }
        }

        mapper.writeValue(suppliers10, JsonTestData(suppliers10list))
    }
}
