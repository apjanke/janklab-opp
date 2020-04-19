/* Glazed Lists                                                 (c) 2003-2005 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.adt.barcode2;

import java.util.List;
import java.util.AbstractList;

/*
 m4_include(source/ca/odell/glazedlists/impl/adt/barcode2/JavaMacros.m4)
 m4_include(source/ca/odell/glazedlists/impl/adt/barcode2/TreeMacros.m4)
*/
/*[ BEGIN_M4_JAVA ]*/

/**
 * Adapt a {@link BciiTree} for use as a {@link List}.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public class BciiTreeAsList<V> extends AbstractList<V> {

    private final BciiTree<V> tree;

    /*[ COLORED_START ]*/
    private final byte colors;

    /** the color of inserted or added elements */
    private final byte color;
    /*[ COLORED_END ]*/

    /*[ COLORED_START ]*/
    /**
     * Create a new {@link BciiTreeAsList} adapting the specified tree.
     */
    public BciiTreeAsList/**/(BciiTree<V> tree) {
        this(tree, tree.getCoder().colorsToByte(tree.getCoder().getColors()), (byte)1);
    }
    /*[ COLORED_END ]*/

    /**
     * Create a new {@link BciiTreeAsList}, adapting the specified colors subset
     * of the specified tree. Inserted elements via {@link #add} will be of the
     * specified color.
     */
    public BciiTreeAsList/**/(BciiTree<V> tree /*[ COLORED_START ]*/ , byte colors, byte color /*[ COLORED_END ]*/) {
        this.tree = tree;
        /*[ COLORED_START ]*/
        this.colors = colors;
        this.color = color;
        /*[ COLORED_END ]*/
    }

    /** {@inheritDoc} */
    public V get(int index) {
        return tree.get(index /*[ COLORED_START ]*/, colors /*[ COLORED_END ]*/).get();
    }

    /** {@inheritDoc} */
    public void add(int index, V element) {
        tree.add(index, /*[ COLORED_START ]*/ colors, color, /*[ COLORED_END ]*/ element, 1);
    }

    /** {@inheritDoc} */
    public V set(int index, V element) {
        V replaced = get(index);
        tree.set(index, /*[ COLORED_START ]*/ colors, color, /*[ COLORED_END ]*/ element, 1);
        return replaced;
    }

    /** {@inheritDoc} */
    public V remove(int index) {
        V removed = get(index);
        tree.remove(index, /*[ COLORED_START ]*/ colors, /*[ COLORED_END ]*/ 1);
        return removed;
    }

    /** {@inheritDoc} */
    public int size() {
        return tree.size(/*[ COLORED_START ]*/ colors /*[ COLORED_END ]*/);
    }
}
/*[ END_M4_JAVA ]*/
