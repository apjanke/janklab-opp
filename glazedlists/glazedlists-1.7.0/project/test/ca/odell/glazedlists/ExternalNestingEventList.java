/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEvent;

/**
 * A list that allows nested events to be managed externally. This is very useful
 * in testing handling of sophisticated events, because it allows big events
 * to be composed from simpler ones.
 */
public class ExternalNestingEventList<E> extends TransformedList<E, E> {
    final boolean forward;
    public ExternalNestingEventList(EventList<E> source) {
        this(source, false);
    }
    public ExternalNestingEventList(EventList<E> source, boolean forward) {
        super(source);
        this.forward = forward;
        source.addListEventListener(this);
    }
    public void beginEvent(boolean allowNested) {
        updates.beginEvent(allowNested);
    }
    public void commitEvent() {
        updates.commitEvent();
    }
    protected boolean isWritable() {
        return true;
    }
    public void listChanged(ListEvent<E> listChanges) {
        if(forward) {
            updates.forwardEvent(listChanges);
        } else {
            // don't forward() the event, just add the changes
            if(listChanges.isReordering()) {
                int[] reorderMap = listChanges.getReorderMap();
                updates.reorder(reorderMap);
            } else {
                while(listChanges.next()) {
                    updates.addChange(listChanges.getType(), listChanges.getIndex());
                }
            }
        }
    }
}