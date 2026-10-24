package com.xichugeek.lunatide.domain

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class LocalContentTest {
    @Test fun everyUiResourceHasAnEnglishTranslationAndOtherPlural() {
        fun read(path: String): Map<String, String> {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
            val values = mutableMapOf<String, String>()
            listOf("string", "plurals").forEach { kind ->
                val nodes = document.getElementsByTagName(kind)
                for (i in 0 until nodes.length) {
                    val node = nodes.item(i) as org.w3c.dom.Element
                    val name = node.getAttribute("name")
                    assertNull("Duplicate resource $name", values.put(name, kind))
                    if (kind == "plurals") {
                        val items = node.getElementsByTagName("item")
                        assertTrue("Missing other plural: $name", (0 until items.length).any { (items.item(it) as org.w3c.dom.Element).getAttribute("quantity") == "other" })
                    }
                }
            }
            return values
        }
        assertEquals(read("src/main/res/values/strings.xml"), read("src/main/res/values-en/strings.xml"))
    }
    @Test fun bundledPolicyAndLicenseInputsContainRequiredDisclosures() {
        val policy = File("../PRIVACY.md").readText()
        listOf("不联网", "Keystore", "界面访问控制", "卸载", "CSV", "删除", "避孕").forEach { assertTrue(policy.contains(it)) }
        val notice = File("../THIRD_PARTY_NOTICES.md").readText()
        listOf("SQLCipher", "ZETETIC LLC", "Redistribution and use", "THIS SOFTWARE IS PROVIDED", "MIT", "Apache License").forEach { assertTrue(notice.contains(it)) }
    }
}
