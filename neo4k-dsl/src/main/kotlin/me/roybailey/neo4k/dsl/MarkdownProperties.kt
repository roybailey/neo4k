package me.roybailey.neo4k.dsl

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream

/**
 * Example of loading properties, including multi-line code blocks, from markdown files.
 *
 * @author Roy Bailey
 */
object MarkdownProperties {

    /**
     * Regular expression to extract code blocks using markdown ` char tokens
     */
    val PATTERN_CODE_BLOCK = "([^`]*[`]{1,3})([^`]*)([`]{1,3}[^`]*)"

    /**
     * Matching helper
     *
     * @param pattern the regular expression to match
     * @param group   the group from the regular expression to extract
     * @param payload the payload to match on
     * @return list of extracted group values
     */
    fun match(pattern: String, group: Int, payload: String): List<String> {
        val code = ArrayList<String>()
        val compiled = Pattern.compile(pattern)
        val matcher = compiled.matcher(payload)
        while (matcher.find() && matcher.group(group).isNotEmpty()) {
            code.add(matcher.group(group))
        }
        return code
    }

    /**
     * Loads properties from markdown, based on simple pairing of code blocks.
     *
     * @param markdown Path object to markdown file content.
     * @return map of properties extracted from markdown.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun loadFromFile(markdown: Path): Map<String, String> {
        return load(Files.lines(markdown, StandardCharsets.UTF_8))
    }

    /**
     * Loads properties from markdown, based on simple pairing of code blocks.
     *
     * @param markdown Path object to markdown file content.
     * @return map of properties extracted from markdown.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun loadFromClasspath(resourcePath: String, clazz: Any = MarkdownProperties): Map<String, String> {
        val resource = clazz::class.java.getResource(resourcePath)
        val text = resource.readText()
        return load(text.split("\n").stream())
    }

    /**
     * Loads properties from markdown, based on simple pairing of code blocks.
     *
     * @param markdown stream of markdown file content lines.
     * @return map of properties extracted from markdown.
     */
    fun load(markdown: Stream<String>): Map<String, String> {
        val result = HashMap<String, String>()
        val name = StringBuilder()
        val buffer = StringBuilder()

        markdown.forEach { line ->
            buffer.append(line).append('\n')
            match(PATTERN_CODE_BLOCK, 2, buffer.toString()).stream()
                    .filter { code -> code.trim { it <= ' ' }.length >= 0 }
                    .forEach { code ->
                        if (name.length == 0) {
                            name.append(code)
                        } else {
                            result[name.toString()] = code
                            name.setLength(0)
                        }
                        buffer.setLength(0)
                    }
        }
        return result
    }
}
