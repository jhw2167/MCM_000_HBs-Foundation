package com.holybuckets.foundation.datastructure;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentCircularList<E> implements List<E> {
    private Node<E> head;
    private int size;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static class Node<E> {
        E element;
        Node<E> next;

        Node(E element) {
            this.element = element;
        }
    }

    public ConcurrentCircularList() {
        head = null;
        size = 0;
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return size == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        lock.readLock().lock();
        try {
            if (head == null) return false;
            Node<E> current = head;
            do {
                if (Objects.equals(current.element, o)) return true;
                current = current.next;
            } while (current != head);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private Node<E> current = head;
            private Node<E> last = null;
            private boolean started = false;

            @Override
            public boolean hasNext() {
                lock.readLock().lock();
                try {
                    return head != null && (!started || current != head);
                } finally {
                    lock.readLock().unlock();
                }
            }

            @Override
            public E next() {
                lock.readLock().lock();
                try {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    started = true;
                    last = current;
                    current = current.next;
                    return last.element;
                } finally {
                    lock.readLock().unlock();
                }
            }
        };
    }

    @Override
    public Object[] toArray() {
        lock.readLock().lock();
        try {
            Object[] result = new Object[size];
            int i = 0;
            for (E e : this) {
                result[i++] = e;
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        lock.readLock().lock();
        try {
            if (a.length < size) {
                a = (T[]) java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
            }
            int i = 0;
            for (E e : this) {
                a[i++] = (T) e;
            }
            if (a.length > size) {
                a[size] = null;
            }
            return a;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean add(E e) {
        lock.writeLock().lock();
        try {
            Node<E> newNode = new Node<>(e);
            if (head == null) {
                head = newNode;
                head.next = head;
            } else {
                Node<E> last = head;
                while (last.next != head) {
                    last = last.next;
                }
                last.next = newNode;
                newNode.next = head;
            }
            size++;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        lock.writeLock().lock();
        try {
            if (head == null) return false;

            Node<E> current = head;
            Node<E> prev = null;
            boolean found = false;

            do {
                if (Objects.equals(current.element, o)) {
                    found = true;
                    break;
                }
                prev = current;
                current = current.next;
            } while (current != head);

            if (!found) return false;

            if (size == 1) {
                head = null;
            } else {
                if (current == head) {
                    prev = head;
                    while (prev.next != head) {
                        prev = prev.next;
                    }
                    head = head.next;
                }
                prev.next = current.next;
            }
            size--;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        lock.readLock().lock();
        try {
            for (Object o : c) {
                if (!contains(o)) return false;
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        lock.writeLock().lock();
        try {
            boolean modified = false;
            for (E e : c) {
                modified |= add(e);
            }
            return modified;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("addAll at index not supported in circular list");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            boolean modified = false;
            for (Object o : c) {
                modified |= remove(o);
            }
            return modified;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            boolean modified = false;
            Iterator<E> it = iterator();
            while (it.hasNext()) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            head = null;
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E get(int index) {
        lock.readLock().lock();
        try {
            checkElementIndex(index);
            Node<E> current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
            return current.element;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E set(int index, E element) {
        lock.writeLock().lock();
        try {
            checkElementIndex(index);
            Node<E> current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
            E oldValue = current.element;
            current.element = element;
            return oldValue;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("add at index not supported in circular list");
    }

    @Override
    public E remove(int index) {
        lock.writeLock().lock();
        try {
            checkElementIndex(index);
            Node<E> current = head;
            Node<E> prev = null;
            
            for (int i = 0; i < index; i++) {
                prev = current;
                current = current.next;
            }

            E oldValue = current.element;
            
            if (size == 1) {
                head = null;
            } else {
                if (current == head) {
                    prev = head;
                    while (prev.next != head) {
                        prev = prev.next;
                    }
                    head = head.next;
                }
                prev.next = current.next;
            }
            size--;
            return oldValue;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        lock.readLock().lock();
        try {
            int index = 0;
            if (head == null) return -1;
            Node<E> current = head;
            do {
                if (Objects.equals(current.element, o)) return index;
                current = current.next;
                index++;
            } while (current != head);
            return -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        lock.readLock().lock();
        try {
            if (head == null) return -1;
            int lastIndex = -1;
            int currentIndex = 0;
            Node<E> current = head;
            do {
                if (Objects.equals(current.element, o)) {
                    lastIndex = currentIndex;
                }
                current = current.next;
                currentIndex++;
            } while (current != head);
            return lastIndex;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException("ListIterator not supported in circular list");
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException("ListIterator not supported in circular list");
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("subList not supported in circular list");
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }
}
