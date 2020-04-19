/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.swt;

// the core Glazed Lists packages
import ca.odell.glazedlists.*;
import ca.odell.glazedlists.gui.*;
import ca.odell.glazedlists.event.*;
// to make of use Barcode
import ca.odell.glazedlists.impl.adt.*;
// SWT toolkit stuff for displaying widgets
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.SelectionListener;

/**
 * A view helper that displays an EventList in an SWT table.
 *
 * <p>This class is not thread safe. It must be used exclusively with the SWT
 * event handler thread.
 *
 * <p><strong>Warning:</strong> This class is a a developer preview and subject to
 * many bugs and API changes.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public class EventTableViewer implements ListEventListener {

    /** the heavyweight table */
    private Table table;

    /** to manipulate Tables in a generic way */
    private TableHandler tableHandler = null;

    /** the first source event list to dispose */
    private TransformedList disposeSource = null;

    /** the proxy moves events to the SWT user interface thread */
    private TransformedList swtSource = null;

    /** Enables check support */
    private TableCheckFilterList checkFilter = null;

    /** Specifies how to render table headers and sort */
    private TableFormat tableFormat;

    /** For selection management */
    private SelectionManager selection = null;

    /**
     * Creates a new viewer for the given {@link Table} that updates the table
     * contents in response to changes on the specified {@link EventList}.  The
     * {@link Table} is formatted with an automatically generated
     * {@link TableFormat}. It uses JavaBeans and Reflection to create a
     * {@link TableFormat} as specified.
     */
    public EventTableViewer(EventList source, Table table, String[] propertyNames, String[] columnLabels) {
        this(source, table, GlazedLists.tableFormat(propertyNames, columnLabels));
    }

    /**
     * Creates a new viewer for the given {@link Table} that updates the table
     * contents in response to changes on the specified {@link EventList}.  The
     * {@link Table} is formatted with the specified {@link TableFormat}.
     */
    public EventTableViewer(EventList source, Table table, TableFormat tableFormat) {
        swtSource = GlazedListsSWT.swtThreadProxyList(source, table.getDisplay());
        disposeSource = swtSource;

        // insert a checked source if supported by the table
        if((table.getStyle() & SWT.CHECK) == SWT.CHECK) {
            checkFilter = new TableCheckFilterList(swtSource, table, tableFormat);
            swtSource = checkFilter;
        }

        // save table, source list and table format
        this.table = table;
        this.tableFormat = tableFormat;

        // enable the selection lists
        selection = new SelectionManager(swtSource, new SelectableTable());

        // configure how the Table will be manipulated
        if((table.getStyle() & SWT.VIRTUAL) == SWT.VIRTUAL) {
            tableHandler = new VirtualTableHandler();
        } else {
            tableHandler = new DefaultTableHandler();
        }

        // setup the Table with initial values
        initTable();
        tableHandler.populateTable();

        // listen for events, using the user interface thread
        swtSource.addListEventListener(this);
    }

    /**
     * Builds the columns and headers for the {@link Table}
     */
    private void initTable() {
        table.setHeaderVisible(true);
        for(int c = 0; c < tableFormat.getColumnCount(); c++) {
            TableColumn column = new TableColumn(table, SWT.LEFT, c);
            column.setText((String)tableFormat.getColumnName(c));
            column.setWidth(80);
        }
    }

    /**
     * Sets all of the column values on a {@link TableItem}.
     */
    private void setItemText(TableItem item, Object value) {
        for(int i = 0; i < tableFormat.getColumnCount(); i++) {
            Object cellValue = tableFormat.getColumnValue(value, i);
            if(cellValue != null) item.setText(i, cellValue.toString());
            else item.setText(i, "");
        }
    }

    /**
     * Gets the {@link TableFormat}.
     */
    public TableFormat getTableFormat() {
        return tableFormat;
    }

    /**
     * Gets the {@link Table} that is being managed by this
     * {@link EventTableViewer}.
     */
    public Table getTable() {
        return table;
    }


    /**
     * Sets this {@link Table} to be formatted by a different
     * {@link TableFormat}.  This method is not yet implemented for SWT.
     */
    public void setTableFormat(TableFormat tableFormat) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set whether this shall show only checked elements.
     */
    public void setCheckedOnly(boolean checkedOnly) {
        checkFilter.setCheckedOnly(checkedOnly);
    }
    /**
     * Get whether this is showing only checked elements.
     */
    public boolean getCheckedOnly() {
        return checkFilter.getCheckedOnly();
    }

    /**
     * Gets all checked items.
     */
    public java.util.List getAllChecked() {
        return checkFilter.getAllChecked();
    }

    /**
     * Get the source of this {@link EventTableViewer}.
     */
    public EventList getSourceList() {
        return swtSource;
    }

    /**
     * Provides access to an {@link EventList} that contains items from the
     * viewed {@link Table} that are not currently selected.
     */
    public EventList getDeselected() {
        return selection.getSelectionList().getDeselected();
    }

    /**
     * Provides access to an {@link EventList} that contains items from the
     * viewed {@link Table} that are currently selected.
     */
    public EventList getSelected() {
        return selection.getSelectionList().getSelected();
    }

    /**
     * When the source list is changed, this forwards the change to the
     * displayed {@link Table}.
     */
    public void listChanged(ListEvent listChanges) {
        swtSource.getReadWriteLock().readLock().lock();
        Barcode deletes = new Barcode();
        deletes.addWhite(0, swtSource.size());
        int firstChange = swtSource.size();
        try {
            // Disable redraws so that the table is updated in bulk
            table.setRedraw(false);

            // Apply changes to the list
            while(listChanges.next()) {
                int changeIndex = listChanges.getIndex();
                int adjustedIndex = deletes.getIndex(changeIndex, Barcode.WHITE);
                int changeType = listChanges.getType();

                // Insert a new element in the Table and the Barcode
                if(changeType == ListEvent.INSERT) {
                    deletes.addWhite(adjustedIndex, 1);
                    tableHandler.addRow(adjustedIndex, swtSource.get(changeIndex));
                    firstChange = Math.min(changeIndex, firstChange);

                // Update the element in the Table
                } else if(changeType == ListEvent.UPDATE) {
                    tableHandler.updateRow(adjustedIndex, swtSource.get(changeIndex));

                // Just mark the element as deleted in the Barcode
                } else if(changeType == ListEvent.DELETE) {
                    deletes.setBlack(adjustedIndex, 1);
                    firstChange = Math.min(changeIndex, firstChange);
                }
            }

            // Process the deletes as a single Table change
            if(deletes.blackSize() > 0) {
                int[] deletedIndices = new int[deletes.blackSize()];
                for(BarcodeIterator i = deletes.iterator(); i.hasNextBlack(); ) {
                    i.nextBlack();
                    deletedIndices[i.getBlackIndex()] = i.getIndex();
                }
                tableHandler.removeAll(deletedIndices);
            }

            // Re-enable redraws to update the table
            table.setRedraw(true);
        } finally {
            swtSource.getReadWriteLock().readLock().unlock();
        }
    }

    /**
     * Inverts the current selection.
     */
    public void invertSelection() {
        selection.getSelectionList().invertSelection();
    }

    /**
     * Releases the resources consumed by this {@link EventTableViewer} so that it
     * may eventually be garbage collected.
     *
     * <p>An {@link EventTableViewer} will be garbage collected without a call to
     * {@link #dispose()}, but not before its source {@link EventList} is garbage
     * collected. By calling {@link #dispose()}, you allow the {@link EventTableViewer}
     * to be garbage collected before its source {@link EventList}. This is
     * necessary for situations where an {@link EventTableViewer} is short-lived but
     * its source {@link EventList} is long-lived.
     *
     * <p><strong><font color="#FF0000">Warning:</font></strong> It is an error
     * to call any method on a {@link EventTableViewer} after it has been disposed.
     */
    public void dispose() {
        tableHandler.dispose();
        selection.dispose();
        disposeSource.dispose();
    }

    /**
     * To use common selectable widget logic in a widget unaware fashion.
     */
    private final class SelectableTable implements Selectable {
        /** {@inheritDoc} */
        public void addSelectionListener(SelectionListener listener) {
            table.addSelectionListener(listener);
        }

        /** {@inheritDoc} */
        public void removeSelectionListener(SelectionListener listener) {
            table.removeSelectionListener(listener);
        }

        /** {@inheritDoc} */
        public int getSelectionIndex() {
            return table.getSelectionIndex();
        }

        /** {@inheritDoc} */
        public int[] getSelectionIndices() {
            return table.getSelectionIndices();
        }

        /** {@inheritDoc} */
        public int getStyle() {
            return table.getStyle();
        }

        /** {@inheritDoc} */
        public void select(int index) {
            table.select(index);
        }

        /** {@inheritDoc} */
        public void deselect(int index) {
            table.deselect(index);
        }
    }

    /**
     * Defines how Tables will be manipulated.
     */
    private interface TableHandler {

        /**
         * Populate the Table with data.
         */
        public void populateTable();

        /**
         * Add a row with the given value.
         */
        public void addRow(int row, Object value);

        /**
         * Update a row with the given value.
         */
        public void updateRow(int row, Object value);

        /**
         * Removes a set of rows in a single call
         */
        public void removeAll(int[] rows);

        /**
         * Disposes of this TableHandler
         */
        public void dispose();
    }

    /**
     * Allows manipulation of standard SWT Tables.
     */
    private final class DefaultTableHandler implements TableHandler {
        /**
         * Populate the Table with initial data.
         */
        public void populateTable() {
            for(int r = 0; r < swtSource.size(); r++) {
                addRow(r, swtSource.get(r));
            }
        }

        /**
         * Adds a row with the given value.
         */
        public void addRow(int row, Object value) {
            TableItem item = new TableItem(table, 0, row);
            setItemText(item, value);
        }

        /**
         * Updates a row with the given value.
         */
        public void updateRow(int row, Object value) {
            TableItem item = table.getItem(row);
            setItemText(item, value);
        }

        /**
         * Removes a set of rows in a single call
         */
        public void removeAll(int[] rows) {
            table.remove(rows);
        }

        /**
         * Disposes of this TableHandler.
         */
        public void dispose() {
            // no-op for default Tables
        }
    }

    /**
     * Allows manipulation of Virtual Tables and handles additional aspects
     * like providing the SetData callback method and tracking which values
     * are Virtual.
     */
    private final class VirtualTableHandler implements TableHandler, Listener {

        /** to keep track of what's been requested */
        private Barcode requested = null;

        /**
         * Create a new VirtualTableHandler.
         */
        public VirtualTableHandler() {
            requested = new Barcode();
            requested.addWhite(0, swtSource.size());
            table.addListener(SWT.SetData, this);
        }

        /**
         * Populate the Table with initial data.
         */
        public void populateTable() {
            table.setItemCount(swtSource.size());
        }

        /**
         * Adds a row with the given value.
         */
        public void addRow(int row, Object value) {
            // Adding before the last non-Virtual value
            if(row <= getLastIndex()) {
                requested.addBlack(row, 1);
                TableItem item = new TableItem(table, 0, row);
                setItemText(item, value);

            // Adding in the Virtual values at the end
            } else {
                requested.addWhite(requested.size(), 1);
                table.setItemCount(table.getItemCount() + 1);
            }
        }

        /**
         * Updates a row with the given value.
         */
        public void updateRow(int row, Object value) {
            // Only set a row if it is NOT Virtual
            if(!isVirtual(row)) {
                requested.setBlack(row, 1);
                TableItem item = table.getItem(row);
                setItemText(item, value);
            }
        }

        /**
         * Removes a set of rows in a single call
         */
        public void removeAll(int[] rows) {
            // Sync the requested barcode to clear values that have been removed
            for(int i = 0;i < rows.length;i++) {
                requested.remove(rows[i] - i, 1);
            }
            table.remove(rows);
        }

        /**
         * Returns the highest index that has been requested or -1 if the
         * Table is entirely Virtual.
         */
        private int getLastIndex() {
            // Everything is Virtual
            if(requested.blackSize() == 0) return -1;

            // Return the last index
            else return requested.getIndex(requested.blackSize() - 1, Barcode.BLACK);
        }

        /**
         * Returns whether a particular row is Virtual in the Table.
         */
        private boolean isVirtual(int rowIndex) {
            return requested.getBlackIndex(rowIndex) == -1;
        }

        /**
         * Respond to requests for values to fill Virtual rows.
         */
        public void handleEvent(Event e) {
            // Get the TableItem from the Table
            TableItem item = (TableItem)e.item;

            // Calculate the index that should be requested because the Table
            // might be sending incorrectly indexed TableItems in the event.
            int whiteIndex = requested.getWhiteIndex(table.getTopIndex(), false);
            int index = requested.getIndex(whiteIndex, Barcode.WHITE);

            // Set the value on the Virtual element
            requested.setBlack(index, 1);
            setItemText(item, swtSource.get(index));
        }

        /**
         * Allows this handler to clean up after itself.
         */
        public void dispose() {
            table.removeListener(SWT.SetData, this);
        }
    }
}
