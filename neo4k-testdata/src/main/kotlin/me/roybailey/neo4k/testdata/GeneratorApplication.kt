package me.roybailey.neo4k.testdata


fun main(args: Array<String>) {

    val generator = Generator()

    generator.generateSuppliers(5) { index, supplier ->
        println("$index) $supplier")
    }

    generator.generateProducts(10) { index, product ->
        println("$index) $product")
    }

    generator.generateSuppliers(5) { index, supplier ->
        println("$index) $supplier")
    }

    generator.generateProducts(10) { index, product ->
        println("$index) $product")
    }

    System.exit(0)
}
