package pfister.quickercrafting.common.util.collection

import java.rmi.UnexpectedException
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet

class IndexedSet<T>(comparator: Comparator<T>) : ConcurrentSkipListSet<T>(comparator), List<T> {
    /**
     * Creates a [Spliterator] over the elements in this list.
     *
     *
     * The `Spliterator` reports [Spliterator.SIZED] and
     * [Spliterator.ORDERED].  Implementations should document the
     * reporting of additional characteristic values.
     *
     * @implSpec
     * The default implementation creates a
     * *[late-binding](Spliterator.html#binding)* spliterator
     * from the list's `Iterator`.  The spliterator inherits the
     * *fail-fast* properties of the list's iterator.
     *
     * @implNote
     * The created `Spliterator` additionally reports
     * [Spliterator.SUBSIZED].
     *
     * @return a `Spliterator` over the elements in this list
     * @since 1.8
     */
    override fun spliterator(): Spliterator<T> = super<ConcurrentSkipListSet>.spliterator()

    override fun indexOf(element: T): Int {
        iterator().withIndex().forEach {
            if (it.value == element) {
                return it.index
            }
        }
        return -1
    }

    override fun lastIndexOf(element: T): Int {
        val list = mutableListOf<T>()
        iterator().forEach { list.add(it) }
        return list.lastIndexOf(element)
    }

    override fun listIterator(): ListIterator<T> {
        val list = mutableListOf<T>()
        iterator().forEach { list.add(it) }
        return list.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<T> {
        val list = mutableListOf<T>()
        iterator().forEach { list.add(it) }
        return list.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        if (toIndex < fromIndex || fromIndex > toIndex || fromIndex == toIndex) return listOf()
        val list = mutableListOf<T>()
        iterator().withIndex().forEach {
            if (it.index in fromIndex until toIndex) {
                list.add(it.value)
            }
        }
        return list
    }

    override fun get(index: Int): T {
        if (index >= size) {
            throw IndexOutOfBoundsException()
        }
        iterator().withIndex().forEach {
            if (it.index == index)
                return it.value
        }
        throw UnexpectedException("What the heck")
    }
}