package com.blamejared.controlling.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.client.settings.KeyBinding;

/**
 * Manages undo and redo functionality for keybinding changes. Maintains two stacks of snapshot groups to enable
 * multi-level undo/redo. Supports both individual changes and batch operations.
 */
public class UndoRedoManager {

    private static final int MAX_HISTORY_SIZE = 50;

    private final Deque<List<KeyBindingSnapshot>> undoStack = new ArrayDeque<>();
    private final Deque<List<KeyBindingSnapshot>> redoStack = new ArrayDeque<>();
    private List<KeyBindingSnapshot> currentBatch = null;

    /**
     * Records a keybinding change by taking a snapshot before the change occurs. This should be called BEFORE modifying
     * the keybinding. If called within a batch (between beginBatch and endBatch), the snapshot is added to the current
     * batch. Otherwise, it's treated as a single-change batch.
     * 
     * @param keyBinding The keybinding that is about to be changed
     */
    public void recordChange(KeyBinding keyBinding) {
        // Take a snapshot of the current state
        KeyBindingSnapshot snapshot = new KeyBindingSnapshot(keyBinding);

        if (currentBatch != null) {
            // We're in a batch operation, add to current batch
            currentBatch.add(snapshot);
        } else {
            // Single change operation, create a batch with one item
            List<KeyBindingSnapshot> batch = new ArrayList<>();
            batch.add(snapshot);
            undoStack.push(batch);

            // Clear redo stack when a new change is made
            redoStack.clear();

            // Limit stack size to prevent unbounded memory growth
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.removeLast();
            }
        }
    }

    /**
     * Discards the most recently recorded change if it matches the current state (i.e., no actual change occurred).
     * This should be called after the change was supposed to happen to verify it actually did.
     */
    public void discardIfUnchanged() {
        if (currentBatch != null) {
            // We're in a batch, can't discard individual items
            return;
        }

        if (undoStack.isEmpty()) {
            return;
        }

        List<KeyBindingSnapshot> lastBatch = undoStack.peek();
        if (lastBatch == null || lastBatch.isEmpty()) {
            return;
        }

        // Check if all snapshots in the batch match current state (no change occurred)
        boolean allUnchanged = true;
        for (KeyBindingSnapshot snapshot : lastBatch) {
            if (!snapshot.matchesCurrentState()) {
                allUnchanged = false;
                break;
            }
        }

        // If nothing changed, remove this entry
        if (allUnchanged) {
            undoStack.pop();
        }
    }

    /**
     * Begins a batch operation. All recordChange calls after this will be grouped together until endBatch is called.
     * The entire batch can be undone/redone as a single operation.
     */
    public void beginBatch() {
        if (currentBatch != null) {
            throw new IllegalStateException("Batch already in progress");
        }
        currentBatch = new ArrayList<>();
    }

    /**
     * Ends a batch operation and commits all recorded changes as a single undoable operation. Snapshots that match
     * their current state (no change occurred) are filtered out. If no batch is in progress, this is a no-op.
     */
    public void endBatch() {
        if (currentBatch == null) {
            return; // No batch in progress, no-op
        }

        if (!currentBatch.isEmpty()) {
            // Filter out unchanged snapshots
            List<KeyBindingSnapshot> changedSnapshots = new ArrayList<>();
            for (KeyBindingSnapshot snapshot : currentBatch) {
                if (!snapshot.matchesCurrentState()) {
                    changedSnapshots.add(snapshot);
                }
            }

            // Only add to undo stack if there were actual changes
            if (!changedSnapshots.isEmpty()) {
                undoStack.push(changedSnapshots);

                // Clear redo stack when a new change is made
                redoStack.clear();

                // Limit stack size to prevent unbounded memory growth
                if (undoStack.size() > MAX_HISTORY_SIZE) {
                    undoStack.removeLast();
                }
            }
        }

        currentBatch = null;
    }

    /**
     * Undoes the most recent keybinding change or batch of changes. After calling this method, the caller is
     * responsible for calling {@link KeyBinding#resetKeyBindingArrayAndHash()} to update the internal vanilla
     * keybinding hash so the changes take effect.
     * 
     * @return The number of keybindings that were restored, or 0 if there is nothing to undo
     * @throws IllegalStateException if called during a batch operation
     */
    public int undo() {
        if (currentBatch != null) {
            throw new IllegalStateException("Cannot undo during a batch operation");
        }

        if (undoStack.isEmpty()) {
            return 0;
        }

        // Pop the batch from undo stack
        List<KeyBindingSnapshot> batch = undoStack.pop();

        // Take snapshots of current state (for redo)
        List<KeyBindingSnapshot> redoBatch = new ArrayList<>();
        for (KeyBindingSnapshot snapshot : batch) {
            redoBatch.add(new KeyBindingSnapshot(snapshot.getKeyBinding()));
        }
        redoStack.push(redoBatch);

        // Restore all snapshots in the batch
        for (KeyBindingSnapshot snapshot : batch) {
            snapshot.restore();
        }

        return batch.size();
    }

    /**
     * Redoes a previously undone keybinding change or batch of changes. After calling this method, the caller is
     * responsible for calling {@link KeyBinding#resetKeyBindingArrayAndHash()} to update the internal vanilla
     * keybinding hash so the changes take effect.
     * 
     * @return The number of keybindings that were restored, or 0 if there is nothing to redo
     * @throws IllegalStateException if called during a batch operation
     */
    public int redo() {
        if (currentBatch != null) {
            throw new IllegalStateException("Cannot redo during a batch operation");
        }

        if (redoStack.isEmpty()) {
            return 0;
        }

        // Pop the batch from redo stack
        List<KeyBindingSnapshot> batch = redoStack.pop();

        // Take snapshots of current state (for undo)
        List<KeyBindingSnapshot> undoBatch = new ArrayList<>();
        for (KeyBindingSnapshot snapshot : batch) {
            undoBatch.add(new KeyBindingSnapshot(snapshot.getKeyBinding()));
        }
        undoStack.push(undoBatch);

        // Restore all snapshots in the batch
        for (KeyBindingSnapshot snapshot : batch) {
            snapshot.restore();
        }

        return batch.size();
    }

    /**
     * Checks if undo is available.
     * 
     * @return true if there are changes that can be undone
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Checks if redo is available.
     * 
     * @return true if there are changes that can be redone
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Clears all undo and redo history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        currentBatch = null;
    }

    /**
     * Gets the number of undo operations available.
     * 
     * @return The size of the undo stack
     */
    public int getUndoCount() {
        return undoStack.size();
    }

    /**
     * Gets the number of redo operations available.
     * 
     * @return The size of the redo stack
     */
    public int getRedoCount() {
        return redoStack.size();
    }
}
