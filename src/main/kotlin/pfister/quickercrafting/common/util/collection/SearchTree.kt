package pfister.quickercrafting.common.util.collection

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import suffixtree.GeneralizedSuffixTree

class SearchTree : GeneralizedSuffixTree() {
    var nextMapIndex = 0
    private val indexGroupingMap: MutableMap<Int, IntList> = mutableMapOf()

    // Puts all the strings in the tree and records their indexes in the grouping map
    private var nextTreeIndex = 0

    fun putGrouping(list: List<String>) {
        val intList = IntArrayList()
        list.forEach {
            intList.push(nextTreeIndex)
            put(it, nextTreeIndex)
            nextTreeIndex += 1
        }
        indexGroupingMap.put(nextMapIndex, intList)
        nextMapIndex++
    }

    fun putGrouping(vararg strings: String) {
        putGrouping(strings.toList())
    }

    // Finds the grouping index from the tree index, or -1 if it cant find anything
    fun getGroupingIndex(treeIndex: Int): Int {
        return indexGroupingMap.entries.find { it.value.contains(treeIndex) }?.key ?: -1
    }
}