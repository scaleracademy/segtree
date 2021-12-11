// Generics in Java

import java.io.*;
import java.lang.*;
import java.util.*;

interface Operator<T> {
    T apply(T leftValue, T rightValue);
}


class SegmentTree<T> {
    private final Operator<T> operator;
    private final T identity;
    private int length; // for the external world
    private int capacity; // for internal use
    private T[] tree;
    private final double LOAD_FACTOR = 2;

    public SegmentTree(List<T> initialValues, Operator<T> operator, T identity) {
        this.operator = operator;
        this.identity = identity;
        length = capacity = initialValues.size();
        tree = (T[]) new Object[2 * capacity - 1]; // 4n
        buildTree(0, capacity - 1, 0, initialValues);
    }

    private int getMid(int start, int end) { return start + (end - start) / 2; }
    private int leftChild(int node) { return node + 1; }
    private int rightChild(int node, int start, int mid) {
        /*
                    1                ----- 0 + 7 + 1 = 8
                  /   \
                 2      e 
                / \     9
               3   6   
              a b  c d
              4 5  7 8
        */
        int numberOfLeavesInTheLeftSubtree = mid - start + 1;
        int numberOfNodesInTheLeftSubtree = 2 * numberOfLeavesInTheLeftSubtree - 1;
        return node + numberOfNodesInTheLeftSubtree + 1;
    }

    private void buildTree(int rangeStart, int rangeEnd, int currentNode, List<T> initialValues) {
        if(rangeStart == rangeEnd) {
            // the currentNode represents a leaf, which has the range of a single element
            tree[currentNode] = initialValues.get(rangeStart);
            return;
        }
        int mid = getMid(rangeStart, rangeEnd);
        // left subtree
        buildTree(rangeStart, mid, leftChild(currentNode), initialValues);
        // right subtree
        buildTree(mid + 1, rangeEnd, rightChild(currentNode, rangeStart, mid), initialValues);
        // combine
        tree[currentNode] = operator.apply(tree[leftChild(currentNode)], tree[rightChild(currentNode, rangeStart, mid)]);
    }

    public T get(int index) {
        if(index < 0 || index >= length) throw new IndexOutOfBoundsException("index: " + index);
        return queryRange(index, index);
    }

    private void resize(int newCapacity) {        
        // take all the previous values
        List<T> values = new ArrayList<>();
        for(int i = 0; i < length; i++)
            values.add(get(i));
        // append a bunch of identities to increase the size
        for(int i = length; i < newCapacity; i++)
            values.add(identity);
        // rebuild the tree with these values                
        tree = (T[]) new Object[2 * newCapacity - 1];
        buildTree(0, newCapacity - 1, 0, values);
        capacity = newCapacity;
    }

    public void add(T value) {
        if(length == capacity) {
            resize((int)(capacity * LOAD_FACTOR) + 1);
        }
        length++;
        set(length - 1, value);
    }

    public void set(int index, T value) {
        if(index < 0 || index >= length) throw new IndexOutOfBoundsException("index: " + index);
        set(index, 0, capacity - 1, 0, value);
    }

    private void set(int index, int rangeStart, int rangeEnd, int currentNode, T value) {
        if(rangeStart == rangeEnd && rangeEnd == index) {
            // the currentNode represents a leaf. and the range contains only the element at the desried index
            tree[currentNode] = value;
            return;
        }
        int mid = getMid(rangeStart, rangeEnd);
        if(index <= mid)
            set(index, rangeStart, mid, leftChild(currentNode), value);
        else
            set(index, mid + 1, rangeEnd, rightChild(currentNode, rangeStart, mid), value);
        // because my children have changed, I gotta update my value as well
        tree[currentNode] = operator.apply(tree[leftChild(currentNode)], tree[rightChild(currentNode, rangeStart, mid)]);
    }

    public T queryRange(int start, int end) {
        if(start < 0 || start >= length) throw new IndexOutOfBoundsException("start: " + start);
        if(end < 0 || end >= length) throw new IndexOutOfBoundsException("end: " + end);
        return queryRange(start, end, 0, capacity - 1, 0);
    }

    private T queryRange(int queryStart, int queryEnd, int rangeStart, int rangeEnd, int currentNode) {
        /*
           qs                   qe
           ----------------------
               -----------
               rs        re
        */
        if(queryStart <= rangeStart && rangeEnd <= queryEnd) {
            // the currentNode represents a range that is fully contained within the query
            // so we need the value of this node completely
            return tree[currentNode];
        }
        /*
              qs       qe  rs       re
              ----------   ----------
              rs       re  qs       qe
              ----------   ----------
        */
        if(queryStart > rangeEnd || rangeStart > queryEnd) {
            // the query has no overlap with the range of the currentNode
            return identity;
        }
        // partial overlap
        int mid = getMid(rangeStart, rangeEnd);
        T leftValue = queryRange(queryStart, queryEnd, rangeStart, mid, leftChild(currentNode));
        T rightValue = queryRange(queryStart, queryEnd, mid + 1, rangeEnd, rightChild(currentNode, rangeStart, mid));
        return operator.apply(leftValue, rightValue);
    }

    public int size() { return length; }

    public int capacity() { return capacity; }

    public Operator<T> getOperator() { return operator; }

    public T getIdentity() { return identity; }

    public void display() {
        System.out.print("[ ");
        for(int i = 0; i < length; i++) {
            System.out.print(get(i) + " ");
        }
        for(int i = length; i < capacity; i++) {
            System.out.print("_ ");
        }
        System.out.println(" ]");
    }
}

class SegmentTreeDeletable<T> {
    private final SegmentTree<T> tree;
    private final SegmentTree<Integer> deletedIndices;
    private int length;

    public SegmentTreeDeletable(List<T> initialValues, Operator<T> operator, T identity) {
        tree = new SegmentTree<>(initialValues, operator, identity);
        List<Integer> deleted = new ArrayList<>();
        for(int i = 0; i < tree.size(); i ++)
            deleted.add(0);
        deletedIndices = new SegmentTree<>(deleted, Integer::sum, 0);
        length = initialValues.size();
    }

    private int getTrueIndex(int index) {
        // trueIndex
        // [ a b c d ]
        //       2 (index)
        // [ 0 1 0 1 0 0]
        // [ a z b s c d]
        //           ^ (trueIndex)
        // trueIndex - index = number of deleted indices in the range (0...trueIndex)
        int low = index;
        int high = tree.size() - 1;
        while(low <= high) {
            int mid = low + (high - low) / 2;
            int deletedCount = deletedIndices.queryRange(0, mid);
            if(mid - index < deletedCount) low = mid + 1;
            else high = mid - 1;
        }
        return low;
    }

    public T queryRange(int start, int end) {
        start = getTrueIndex(start);
        end = getTrueIndex(end);
        return tree.queryRange(start, end);
    }
    public void set(int index, T value) {
        index = getTrueIndex(index);
        tree.set(index, value);
    }
    public void add(T value) {
        tree.add(value);
        deletedIndices.add(0); // the newly made index has not been deleted
        length++;
    }
    public T get(int index) {
        index = getTrueIndex(index);
        return tree.get(index);
    }
    public void delete(int index) {
        index = getTrueIndex(index);
        // mark this index as deleted
        deletedIndices.set(index, 1);
        // set the value to identity
        tree.set(index, tree.getIdentity());
        length--;
    }

    public int size() { return length; }

    public int capacity() { 
        int capacity = tree.capacity();
        int countDeleted = deletedIndices.queryRange(0, capacity);
        return capacity - countDeleted; 
    }

    public Operator<T> getOperator() { return tree.getOperator(); }
    
    public T getIdentity() { return tree.getIdentity(); }

    public void display() {
        tree.display();
        deletedIndices.display();
        System.out.print("Apparent array [ ");
        for(int i = 0; i < length; i++) {
            System.out.print(get(i) + " ");
        }
        System.out.println(" ]");
    }
}


public class SegtreeDemo {
    public static void main(String args[]) {
        SegmentTreeDeletable<Integer> tree = new SegmentTreeDeletable<>(Arrays.asList(1, 2), Integer::sum, 0);
        
        for(int i = 1; i < 10; i++) {
            tree.display();
            tree.add(i * 10);
            if(i % 3 == 0){
                tree.display();
                tree.delete(0);
            }

        }
        tree.display();
    }
}