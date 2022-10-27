package org.jobrunr.stubs;

import java.util.ArrayList;
import java.util.List;

public class TaskEvent {

    public List<Task> tasks = new ArrayList<>();

    public static abstract class Task {

        public void process(String id) {
            processImpl(id);
        }

        protected abstract void processImpl(String id);
    }

    public static class Task1 extends Task {

        @Override
        public void processImpl(String id) {
            System.out.println("Task 1: " + id);
        }
    }

    public static class Task2 extends Task {

        @Override
        public void processImpl(String id) {
            System.out.println("Task 2:" + id);
        }
    }
}

