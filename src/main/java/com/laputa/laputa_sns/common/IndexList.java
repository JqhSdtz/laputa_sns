package com.laputa.laputa_sns.common;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author JQH
 * @since 下午 3:59 20/02/22
 */

public class IndexList {

    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private class Node {
        Node left, right;
        Index data;
        boolean isLinked = false;

        Node(Index data) {
            this.data = data;
        }
    }

    public IndexList(int capacity) {
        nodeMap = new HashMap((int) (capacity / 0.75f));
        lPole = new Node(null);
        rPole = new Node(null);
        lPole.right = rPole;
        rPole.left = lPole;
        lPole.isLinked = true;
        rPole.isLinked = true;
    }

    private Node lPole, rPole;//左右两极
    private Map<Integer, Node> nodeMap;
    private int size = 0;

    @NotNull
    @Contract("_ -> param1")
    //这里不能删除后把node设为null，因为遍历的时候还会用到node，等gc回收即可
    private Node unlink(@NotNull Node node) {
        if (!node.isLinked || node == lPole || node == rPole)
            return null;
        node.isLinked = false;
        Node leftNode = node.left;
        Node rightNode = node.right;
        leftNode.right = rightNode;
        rightNode.left = leftNode;
        return node;
    }

    private boolean unlinkAndLink(Node node, Node des, int dir) {
        if (unlink(node) == null)
            return false;
        return dir == LEFT ? linkAfter(node, des) : linkBefore(node, des);
    }

    //从左到右，由大到小
    private boolean sort(Node node, int dir, boolean isSync) {
        if (dir == LEFT) {//值增长，向左比较
            Node des = node.left;
            while (des != lPole && node.data.compareTo(des.data) < 0)//compareTo小于0表示大于参数，遇到大于或等于当前值的则停止
                des = des.left;
            if (des == node.left)
                return node.isLinked;
            if (isSync)//调用时已经是同步
                return unlinkAndLink(node, des, dir);
            synchronized (this) {
                return unlinkAndLink(node, des, dir);
            }
        } else if (dir == RIGHT) {//值减小，向右比较
            Node des = node.right;
            while (des != rPole && node.data.compareTo(des.data) > 0)//compareTo大于0表示小于参数，遇到小于或等于当前值的则停止
                des = des.right;
            if (des == node.right)
                return node.isLinked;
            if (isSync)
                return unlinkAndLink(node, des, dir);
            synchronized (this) {
                return unlinkAndLink(node, des, dir);
            }
        } else
            return false;
    }

    private boolean linkAfter(@NotNull Node node, @NotNull Node left) {
        if (node.isLinked || !left.isLinked || left == rPole)
            return false;
        node.right = left.right;
        node.left = left;
        left.right.left = node;
        left.right = node;
        node.isLinked = true;
        return true;
    }

    private boolean linkBefore(@NotNull Node node, @NotNull Node right) {
        if (node.isLinked || !right.isLinked || right == lPole)
            return false;
        node.left = right.left;
        node.right = right;
        right.left.right = node;
        right.left = node;
        node.isLinked = true;
        return true;
    }


    public synchronized Index addLast(@NotNull Index data, boolean reqSort) {
        if (nodeMap.containsKey(data.getId()))//已经有这个索引了
            return data;
        Node node = new Node(data);
        linkBefore(node, rPole);
        if (reqSort && !sort(node, LEFT, true))
            return null;//插入后被另一线程删除
        nodeMap.put(node.data.getId(), node);
        ++size;
        return node.data;
    }

    public synchronized Index addFirst(@NotNull Index data, boolean reqSort) {
        if (nodeMap.containsKey(data.getId()))//已经有这个索引了
            return data;
        Node node = new Node(data);
        linkAfter(node, lPole);
        if (reqSort && !sort(node, RIGHT, true))
            return null;
        nodeMap.put(node.data.getId(), node);
        ++size;
        return node.data;
    }

    public synchronized Index popLast() {
        if (size == 0)
            return null;
        Index tmp = unlink(rPole.left).data;
        nodeMap.remove(tmp.getId());
        --size;
        return tmp;
    }

    public synchronized Index remove(int id) {
        if (!nodeMap.containsKey(id))
            return null;
        Index tmp = unlink(nodeMap.get(id)).data;
        nodeMap.remove(tmp.getId());
        --size;
        return tmp;
    }

    public boolean update(@NotNull Index index, boolean reqSort, int dir) {
        Node node = nodeMap.get(index.getId());
        if (node == null)
            return false;
        node.data.setValue(index.getValue());
        if (reqSort)
            return sort(node, dir, false);
        return true;
    }

    public boolean hasIndex(int id) {
        return nodeMap.containsKey(id);
    }

    public int size() {
        return size;
    }

    public Index getFirst() {
        return lPole.right.data;
    }

    public Index getLast() {
        return rPole.left.data;
    }

    /**若getRemained为true则返回缩减后剩余的节点，否则返回被缩减掉的节点*/
    public synchronized List<Index> trim(int size, boolean getRemained) {
        Node node = lPole;
        List<Index> remainedList = getRemained ? new ArrayList() : null;
        for (int i = 0; i < size; ++i) {//定位到要保留的最后一个节点
            node = node.right;
            if (node == null || node == rPole)
                break;
            if (getRemained)
                remainedList.add(node.data);
        }
        if (node == null || node == rPole)
            return getRemained ? remainedList : new ArrayList(0);
        Node trimNode = node.right;//记录下最后一个节点的下一个
        //把最后一个节点连到右端点上
        node.right = rPole;
        rPole.left = node;
        //删除掉最后一个节点之后的所有节点
        List<Index> removedList = getRemained ? null : new ArrayList();
        while(trimNode != null && trimNode.data != null) {
            if (!getRemained)
                removedList.add(trimNode.data);
            nodeMap.remove(trimNode.data.getId());
            trimNode = trimNode.right;
        }
        this.size = size;
        return getRemained ? remainedList : removedList;
    }

    public Iterator<Index> iterator() {
        return new IndexIterator();
    }

    /**
     * 返回的节点是根据startId和from找到的节点的下一个节点
     */
    public Iterator<Index> iterator(Integer startId, Integer from) {
        return new IndexIterator(startId, from);
    }

    private class IndexIterator implements Iterator<Index> {

        private Node cur;

        public IndexIterator() {
            cur = lPole.right;
        }

        public IndexIterator(Integer startId, Integer from) {//优先根据startId获取迭代器，若结果为null，再尝试根据from参数获取
            Node node = null;
            if (startId != null)
                node = startId.equals(0) ? lPole : nodeMap.get(startId);
            if (node == null && from != null && from < size) {
                node = lPole;
                for (int i = 0; i < from && node != null; ++i)//from等于1的话,node是第一个帖子
                    node = node.right;
            }
            cur = node == null ? rPole : node.right;//若startId和from都找不到节点，则返回rPole，即末尾，调用方得不到任何数据
        }

        public boolean hasNext() {
            return cur != rPole;
        }

        //这种方式有一定隐患，如果在遍历的过程中当前节点被移动到另一个位置，则原位置和新位置之间的节点将被忽略
        //可以使用CAS操作判断当前节点是否被移动，若被移动则返回空，告诉调用方重新遍历，但目前的并发量没有这个必要
        public Index next() {
            Index data = cur.data;
            cur = cur.right;
            return data;
        }

        public void remove() {
            IndexList.this.remove(cur.data.getId());
        }
    }

}
