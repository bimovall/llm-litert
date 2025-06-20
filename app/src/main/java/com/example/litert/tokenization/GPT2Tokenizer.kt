package com.example.litert.tokenization

class GPT2Tokenizer(
    val encoder: Map<String, Int>,
    private val decoder: Map<Int, String>,
    private val bpeRanks: Map<Pair<String, String>, Int>) {
    private val encodeRegex = Regex("""<\|[a-zA-Z0-9_]+\|>|'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+""")
    //private val encodeRegex = Regex("""'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+""")

    fun decode(tokens: List<Int>): String {
        val text = tokens.joinToString("") { decoder.getOrDefault(it, "") }
        val utfCodepoints = text.map { byteDecoder[it.toString()]!! }
        return String(utfCodepoints.toIntArray(), 0, utfCodepoints.size)
    }

    fun decodeFullText(tokens: List<Int>): String {
        val text = tokens.joinToString("") { decoder.getOrDefault(it, "") }
        val utfCodepoints = text.map { byteDecoder[it.toString()]!! }

        return String(utfCodepoints.toIntArray(), 0, utfCodepoints.size)
    }

//    fun encode(text: String): MutableList<Int> {
//        val tokens = mutableListOf<Int>()
//
//        // Pre-sort all encoder keys by length descending to prioritize longest match
//        val encoderKeys = encoder.keys.sortedByDescending { it.length }
//
//        var i = 0
//        while (i < text.length) {
//            var matched: String? = null
//
//            // Try to match the longest encoder key at current position
//            for (key in encoderKeys) {
//                if (i + key.length <= text.length && text.startsWith(key, i)) {
//                    matched = key
//                    break
//                }
//            }
//
//            val byteEncoder = buildByteEncoder()
//
//            if (matched != null) {
//                tokens.add(encoder[matched]!!)
//                i += matched.length
//            } else {
//                // No full encoder key matched — fallback to regex tokenization
//                val subText = text.substring(i)
//                val match = encodeRegex.find(subText)
//                if (match != null && match.range.first == 0) {
//                    val part = match.value
//
//                    val byteStr = part.toByteArray(Charsets.UTF_8)
//                        .map { byteEncoder[it.toInt() and 0xFF]!! }
//                        .joinToString("")
//
//                    val bpeTokens = bpe(byteStr).map { encoder[it]!! }
//                    tokens.addAll(bpeTokens)
//                    i += part.length
//                } else {
//                    // fallback: encode a single character
//                    val ch = text[i].toString()
//                    val byteStr = ch.codePoints()
//                        .boxed()
//                        .map { byteEncoder[it]!! }
//                        .toArray()
//                        .joinToString("")
//                    val bpeTokens = bpe(byteStr).map { encoder[it]!! }
//                    tokens.addAll(bpeTokens)
//                    i++
//                }
//            }
//        }
//
//        return tokens
//    }

    fun encodeFullText(text: String): MutableList<Int> {
        val left = "<|endoftext|><|im_start|>user"
        val tokensLeft = encodeRegex.findAll(left).map { result ->
            encoder[result.value]!!
        }
        val tokenNewLine = 198
        val content = encode(text)

        val tokens = mutableListOf<Int>()
        tokens.addAll(tokensLeft)
        tokens.add(tokenNewLine)
        tokens.addAll(content)
        tokens.add(encoder["<|im_end|>"]!!)
        tokens.add(tokenNewLine)
        tokens.add(encoder["<|im_start|>"]!!)
        tokens.addAll(encode("assistant"))
        tokens.add(tokenNewLine)

        return tokens
    }

    fun encode(text: String): MutableList<Int> {
        val tokens = encodeRegex.findAll(text).map { result ->
            result.value.codePoints()
                .boxed()
                .map { byteEncoder[it] }
                .toArray()
                .joinToString("")
        }


        return tokens
            .map { bpe(it) }
            .flatten()
            .map {
                encoder[it]!!
            }
            .toMutableList()
    }

//    private fun bpe(token: String): List<String> {
//        if (token.length <= 1) return listOf(token)
//
//        var word = token.map { it.toString() }.toMutableList()
//        var pairs = getPairs(word)
//
//        while (true) {
//            val candidate = pairs.minByOrNull { bpeRanks[it] ?: Int.MAX_VALUE } ?: break
//            if (candidate !in bpeRanks) break
//
//            val (first, second) = candidate
//            val newWord = mutableListOf<String>()
//
//            var i = 0
//            while (i < word.size) {
//                if (i < word.size - 1 && word[i] == first && word[i + 1] == second) {
//                    newWord.add(first + second)
//                    i += 2
//                } else {
//                    newWord.add(word[i])
//                    i += 1
//                }
//            }
//
//            word = newWord
//            if (word.size == 1) break
//            pairs = getPairs(word)
//        }
//
//        return word
//    }


    private fun bpe(token: String): List<String> {
        if (token.length <= 1) return listOf(token)
        var word = token.map {
            it.toString()
        }

        var pairs = getPairs(word)

        while (true) {
            if (!pairs.any { bpeRanks.containsKey(it) }) break
            val (first, second) = pairs.minBy { bpeRanks.getOrDefault(it, Int.MAX_VALUE) } ?: break

            var i = 0
            val newWord = mutableListOf<String>()
            while (i < word.size) {
                val j = word.withIndex().indexOfFirst { it.index >= i && it.value == first }
                if (j != -1) {
                    newWord.addAll(word.subList(i, j))
                    i = j
                } else {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }

                if (word[i] == first && i < word.size-1 && word[i+1] == second) {
                    newWord.add(first+second)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i += 1
                }
            }

            word = newWord
            if (word.size == 1) {
                break
            } else {
                pairs = getPairs(word)
            }
        }

        return word
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        return mutableSetOf<Pair<String, String>>().apply {
            for (i in 0 until word.size-1) {
                add(word[i] to word[i+1])
            }
        }
    }

    fun buildByteEncoder(): Map<Int, String> {
        val bs = mutableListOf<Int>()
        val cs = mutableListOf<Int>()

        // Add printable bytes directly
        for (b in 33..126) bs.add(b)
        for (b in 161..172) bs.add(b)
        for (b in 174..255) bs.add(b)

        cs.addAll(bs)

        // Add the rest of the bytes and assign new Unicode chars
        var n = 0
        for (b in 0..255) {
            if (!bs.contains(b)) {
                bs.add(b)
                cs.add(256 + n)
                n++
            }
        }

        return bs.zip(cs.map { it.toChar().toString() }).toMap()
    }
}