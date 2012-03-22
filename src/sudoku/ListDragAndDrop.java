/*
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package sudoku;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import javax.swing.JList;
import javax.swing.JPanel;

/**
 *
 * @author hobiwan
 */
public class ListDragAndDrop implements DragSourceListener, DropTargetListener, DragGestureListener {
    private static final DataFlavor stepConfigDataFlavor;
    private static final DataFlavor[] supportedFlavors;
    
    private DragSource dragSource;
    private DropTarget dropTarget;
    private int draggedIndex = -1;
    private StepConfig dropTargetCell;
    private JList list;
    private ListDragAndDropChange panel;
    private JPanel cPanel;
    
    static {
        stepConfigDataFlavor = new DataFlavor(StepConfig.class, "sudoku.StepConfig");
        supportedFlavors = new DataFlavor[] { stepConfigDataFlavor };
    }
    
    @SuppressWarnings("LeakingThisInConstructor")
    public ListDragAndDrop(JList list, ListDragAndDropChange panel, JPanel cPanel) {
        this.list = list;
        this.panel = panel;
        this.cPanel = cPanel;
        
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(list, DnDConstants.ACTION_MOVE, this);
        dropTarget = new DropTarget(list, this);
    }

    @Override
    public void dragEnter(DragSourceDragEvent dsde) {
        //System.out.println("dragEnter(DragSourceDragEvent)");
//        DragSourceContext context = dsde.getDragSourceContext();
//        //intersection of the users selected action, and the source and target actions
//        int myaction = dsde.getDropAction();
//        if ((myaction & DnDConstants.ACTION_MOVE) != 0) {
//            context.setCursor(DragSource.DefaultMoveDrop);
//        } else {
//            context.setCursor(DragSource.DefaultMoveNoDrop);
//        }
    }

    @Override
    public void dragOver(DragSourceDragEvent dsde) {
        //System.out.println("dragOver(DragSourceDragEvent)");
    }

    @Override
    public void dropActionChanged(DragSourceDragEvent dsde) {
        //System.out.println("dropActionChanged(DragSourceDragEvent)");
    }

    @Override
    public void dragExit(DragSourceEvent dse) {
        //System.out.println("dragExit(DragSourceDragEvent)");
    }

    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
        //System.out.println("dragDropEnd(DragSourceDragEvent)");
        dropTargetCell = null;
        draggedIndex = -1;
        panel.setDropLocation(-1, null);
        list.repaint();
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        //System.out.println("dragEnter(DropTargetDragEvent)");
        if (dtde.getSource() != dropTarget) {
            dtde.rejectDrag();
            panel.setDropLocation(-1, null);
        } else {
            dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            //System.out.println("accepted dragEnter");
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        //System.out.println("dragOver(DropTargetDragEvent)");
        if (dtde.getSource() != dropTarget) {
            dtde.rejectDrag();
            panel.setDropLocation(-1, null);
            return;
        }
        Point dragPoint = dtde.getLocation();
        int index = list.locationToIndex(dragPoint);
        if (index == -1) {
            dropTargetCell = null;
        } else {
            dropTargetCell = (StepConfig) list.getModel().getElementAt(index);
        }
        panel.setDropLocation(index, dropTargetCell);
        list.repaint();
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        //System.out.println("dragActionChanged(DropTargetDragEvent)");
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        //System.out.println("dragExit(DropTargetDragEvent)");
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        //System.out.println("drop(DropTargetDragEvent)");
        if (dtde.getSource() != dropTarget) {
            dtde.rejectDrop();
            panel.setDropLocation(-1, null);
            return;
        }
        Point dropPoint = dtde.getLocation();
        int index = list.locationToIndex(dropPoint);
        boolean dropped = false;
        if (index == -1 || index == draggedIndex) {
            //System.out.println("dropped onto self");
            dtde.rejectDrop();
            panel.setDropLocation(-1, null);
            return;
        }
        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
        //System.out.println("accepted: " + draggedIndex + "/" + index);
        panel.setDropLocation(-1, null);
        panel.moveStep(draggedIndex, index);
        dropped = true;
        dtde.dropComplete(dropped);
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        //System.out.println("dragGestureRecognized");
        Point clickPoint = dge.getDragOrigin();
        int index = list.locationToIndex(clickPoint);
        if (index == -1) {
            return;
        }
        StepConfig target = (StepConfig) list.getModel().getElementAt(index);
        Transferable trans = new RJLTransferable(target);
        draggedIndex = index;
        dragSource.startDrag(dge, Cursor.getDefaultCursor(), trans, this);
        //dragSource.startDrag(dge, DragSource.DefaultMoveDrop, trans, this);
    }

    class RJLTransferable implements Transferable {

        private StepConfig object;

        RJLTransferable(StepConfig object) {
            this.object = object;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return supportedFlavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(stepConfigDataFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return object;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }
}
