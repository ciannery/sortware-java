package com.sortware.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Sorting engine (pure Java, no Android code). Real implementations, no built-in sort. */
public final class Sorter {
    public static final int ANIM_LIMIT = 100; // lists up to this size are animated; bigger lists show real results only
    public static final String[] KEYS = {"bubble", "selection", "merge", "insertion", "quick", "heap"};
    public static final String[] NAME = {"Bubble Sort", "Selection Sort", "Merge Sort", "Insertion Sort", "Quick Sort", "Heap Sort"};
    public static final String[] AUX = {"O(1)", "O(1)", "O(n)", "O(1)", "O(log n)", "O(1)"};
    // best, average, worst, space, stable, in-place, how, pros, cons
    public static final String[][] INFO = {
        {"O(n)", "O(n²)", "O(n²)", "O(1)", "Stable", "Yes", "Compares adjacent items and swaps them if out of order; the largest unsorted value bubbles to the end each pass.", "Simple; stops early on sorted input.", "Very slow on large or reversed data."},
        {"O(n²)", "O(n²)", "O(n²)", "O(1)", "Unstable", "Yes", "Finds the minimum of the unsorted part and swaps it to the front each pass.", "At most n−1 swaps; simple.", "Always O(n²) comparisons; unstable."},
        {"O(n log n)", "O(n log n)", "O(n log n)", "O(n)", "Stable", "No", "Splits the array in halves recursively, then merges the sorted halves.", "Predictable speed; stable.", "Needs extra memory; more overhead on small inputs."},
        {"O(n)", "O(n²)", "O(n²)", "O(1)", "Stable", "Yes", "Takes each element as a key and shifts larger sorted elements right to insert it.", "Fast on small or nearly sorted data; stable.", "Slow on large or reversed data."},
        {"O(n log n)", "O(n log n)", "O(n²)", "O(log n)", "Unstable", "Yes", "Picks a pivot (last element here), partitions smaller and larger values around it, then recurses.", "Usually very fast in practice; in-place.", "Worst case O(n²) with bad pivots; unstable."},
        {"O(n log n)", "O(n log n)", "O(n log n)", "O(1)", "Unstable", "Yes", "Builds a max-heap, then repeatedly swaps the root to the end and re-heapifies.", "Guaranteed O(n log n); in-place.", "Unstable; poor cache behaviour."}};
    public static final String[][] OV = {
        {"Like neighbours in a queue swapping places when the taller one stands in front. After each pass the tallest ends up at the back.", "Each pass compares every neighbouring pair. After pass 1 the biggest number is at the end, after pass 2 the second biggest, and so on. If a whole pass makes no swap, the list is sorted and we stop early."},
        {"Like picking the shortest person from a crowd, sending them to the front, then repeating with everyone left.", "We scan the unsorted part for the smallest value, swap it into the first unsorted spot, and shrink the unsorted part by one."},
        {"Like splitting exam papers among friends: each sorts a small pile, then you merge the piles by always taking the smaller top paper.", "Divide and conquer: split until every piece has one item (already sorted), then merge neighbouring pieces back together in order. Merging needs extra memory."},
        {"Like sorting playing cards in your hand: pick up a card and slide it into the right place among the cards you already hold.", "The left side is always sorted. We take the next number (the key), shift bigger sorted numbers right, and drop the key into the gap."},
        {"Like choosing one person as the ‘pivot’ and asking everyone shorter to stand on the left and everyone taller on the right.", "We pick a pivot (the last item here), move smaller values before it and larger after it. The pivot is then in its final place. We repeat on the left and right parts."},
        {"Like a tournament bracket: the biggest number rises to the top, is sent to the end, and the bracket is repaired.", "First we arrange the numbers into a ‘max-heap’ (every parent is larger than its children). Then we repeatedly swap the top (the biggest) to the end and repair the heap for what remains."}};
    public static final String[][] PSEUDO = {
        {"for i = 0 to n-2:                  // pass", "  swapped = false", "  for j = 0 to n-2-i:", "    if a[j] > a[j+1]:             // compare", "      swap(a[j], a[j+1]); swapped = true", "  if not swapped: break           // end of pass"},
        {"for i = 0 to n-2:                  // pass", "  min = i", "  for j = i+1 to n-1:", "    if a[j] < a[min]: min = j     // compare", "  if min != i: swap(a[i], a[min])", "  a[i] is now sorted"},
        {"mergeSort(lo, hi):", "  if lo >= hi: return", "  mid = (lo + hi) / 2             // split", "  mergeSort(lo, mid); mergeSort(mid+1, hi)", "  merge(lo, mid, hi):", "    while both halves have items: // compare", "      a[k] = smaller front item   // place", "    copy remaining items          // copy"},
        {"for i = 1 to n-1:                  // iteration", "  key = a[i]; j = i-1", "  while j >= 0 and a[j] > key:    // compare", "    a[j+1] = a[j]; j = j-1        // shift", "  a[j+1] = key                    // insert"},
        {"quickSort(lo, hi):", "  if lo >= hi: return", "  pivot = a[hi]; i = lo           // pivot", "  for j = lo to hi-1:", "    if a[j] < pivot: swap(a[i], a[j]); i++", "  swap(a[i], a[hi])               // pivot into place", "  quickSort(lo, i-1); quickSort(i+1, hi)"},
        {"buildMaxHeap: for i = n/2-1 down to 0: heapify(n, i)", "for end = n-1 down to 1:", "  swap(a[0], a[end])              // extract max", "  heapify(end, 0)", "heapify(m, i):", "  largest = i; l = 2i+1; r = 2i+2", "  if l < m and a[l] > a[largest]: largest = l", "  if r < m and a[r] > a[largest]: largest = r", "  if largest != i: swap(a[i], a[largest]); heapify(m, largest)"}};

    public static final class Step {
        public final int[] a, cmp, swp, exm; public final boolean[] srt; public final String lab, msg; public final int c, s, w, p;
        Step(int[] a, int[] cmp, int[] swp, int[] exm, boolean[] srt, String lab, String msg, int c, int s, int w, int p) {
            this.a = a; this.cmp = cmp; this.swp = swp; this.exm = exm; this.srt = srt; this.lab = lab; this.msg = msg; this.c = c; this.s = s; this.w = w; this.p = p;
        }
    }
    public static final class Result {
        public final int[] a; public final int c, s, w, p; public final List<Step> steps;
        Result(int[] a, int c, int s, int w, int p, List<Step> steps) { this.a = a; this.c = c; this.s = s; this.w = w; this.p = p; this.steps = steps; }
    }

    public static int keyIndex(String k) { for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(k)) return i; return 0; }
    public static boolean has(int[] arr, int v) { for (int x : arr) if (x == v) return true; return false; }

    private static final int[] E = new int[0];
    private final int[] a; private final int n; private final boolean rec;
    private int c, s, w, p, rc; private String lab = "Start", ph = "Build Heap";
    private final boolean[] done; private final ArrayList<Step> steps = new ArrayList<>();

    private Sorter(int[] in, boolean rec) { a = in.clone(); n = a.length; done = new boolean[n]; this.rec = rec; }
    private static int[] L(int... x) { return x; }
    private void R(int[] cmp, int[] swp, int[] exm, String msg) { steps.add(new Step(a.clone(), cmp, swp, exm, done.clone(), lab, msg, c, s, w, p)); }
    private void sw(int i, int j, String msg) { int t = a[i]; a[i] = a[j]; a[j] = t; s++; w += 2; if (rec) R(E, L(i, j), E, msg); }
    private void P(String l) { p++; lab = l; }
    private static int[] range(int lo, int hi) { int[] r = new int[hi - lo + 1]; for (int i = 0; i < r.length; i++) r[i] = lo + i; return r; }

    /** Runs one algorithm. rec = record every step for the animation. */
    public static Result run(String name, int[] input, boolean rec) {
        Sorter z = new Sorter(input, rec);
        if (rec) z.R(E, E, E, "Original dataset");
        switch (name) {
            case "bubble": z.bubble(); break;
            case "selection": z.selection(); break;
            case "insertion": z.insertion(); break;
            case "merge": z.ms(0, z.n - 1); break;
            case "quick": z.qs(0, z.n - 1); break;
            default: z.heap();
        }
        for (int k = 0; k < z.n; k++) z.done[k] = true;
        if (rec) z.R(E, E, E, "Sorted!");
        return new Result(z.a, z.c, z.s, z.w, z.p, z.steps);
    }

    private void bubble() {
        for (int i = 0; i < n - 1; i++) {
            P("Pass " + (i + 1)); boolean f = false;
            for (int j = 0; j < n - 1 - i; j++) {
                c++; int x = a[j], y = a[j + 1];
                if (rec) R(L(j, j + 1), E, E, "Compare " + x + " and " + y);
                if (x > y) { f = true; sw(j, j + 1, rec ? x + " > " + y + " → Swap" : null); }
                else if (rec) R(E, E, E, x + " ≤ " + y + " → No swap");
            }
            done[n - 1 - i] = true;
            if (rec) R(E, E, E, "End of pass: " + a[n - 1 - i] + " is in its final position");
            if (!f) break;
        }
    }
    private void selection() {
        for (int i = 0; i < n - 1; i++) {
            P("Pass " + (i + 1)); int m = i;
            if (rec) R(E, E, L(i), "Assume " + a[i] + " (index " + i + ") is the minimum");
            for (int j = i + 1; j < n; j++) {
                c++; if (rec) R(L(j, m), E, E, "Compare " + a[j] + " with current minimum " + a[m]);
                if (a[j] < a[m]) { m = j; if (rec) R(E, E, L(m), "New minimum: " + a[m]); }
            }
            if (m != i) sw(i, m, rec ? "Swap " + a[i] + " and minimum " + a[m] : null);
            else if (rec) R(E, E, L(i), a[i] + " is already the minimum");
            done[i] = true; if (rec) R(E, E, E, a[i] + " is now in its final position");
        }
    }
    private void insertion() {
        for (int i = 1; i < n; i++) {
            P("Iteration " + i); int k = a[i], j = i - 1;
            if (rec) R(E, E, L(i), "Select key " + k);
            while (j >= 0) {
                c++; if (rec) R(L(j), E, L(j + 1), "Compare " + a[j] + " with key " + k);
                if (a[j] > k) { a[j + 1] = a[j]; w++; if (rec) R(E, L(j + 1), E, "Shift " + a[j] + " one place right"); j--; }
                else break;
            }
            if (j + 1 != i) { a[j + 1] = k; w++; }
            if (rec) R(E, E, L(j + 1), "Insert key " + k + " at index " + (j + 1));
        }
    }
    private void ms(int lo, int hi) {
        if (lo >= hi) return;
        int mid = (lo + hi) / 2; lab = "Recursive Call " + (++rc);
        if (rec) R(E, E, range(lo, hi), "Split [" + lo + ".." + hi + "] into [" + lo + ".." + mid + "] and [" + (mid + 1) + ".." + hi + "]");
        ms(lo, mid); ms(mid + 1, hi);
        P("Merge Step " + (p + 1));
        int[] A = java.util.Arrays.copyOfRange(a, lo, mid + 1), B = java.util.Arrays.copyOfRange(a, mid + 1, hi + 1);
        int i = 0, j = 0, k = lo;
        if (rec) R(E, E, range(lo, hi), "Merge [" + join(A) + "] and [" + join(B) + "]");
        while (i < A.length && j < B.length) {
            c++; if (rec) R(L(lo + i, mid + 1 + j), E, E, "Compare " + A[i] + " and " + B[j]);
            a[k] = (A[i] <= B[j]) ? A[i++] : B[j++]; w++;
            if (rec) R(E, E, L(k), "Place " + a[k] + " at index " + k); k++;
        }
        while (i < A.length) { a[k] = A[i++]; w++; if (rec) R(E, E, L(k), "Copy remaining " + a[k] + " to index " + k); k++; }
        while (j < B.length) { a[k] = B[j++]; w++; if (rec) R(E, E, L(k), "Copy remaining " + a[k] + " to index " + k); k++; }
    }
    private void qs(int lo, int hi) {
        if (lo > hi) return;
        if (lo == hi) { done[lo] = true; return; }
        P("Partition " + (p + 1)); int pv = a[hi], i = lo;
        if (rec) R(E, E, L(hi), "Pivot = " + pv + " (index " + hi + "), range [" + lo + ".." + hi + "]");
        for (int j = lo; j < hi; j++) {
            c++; if (rec) R(L(j, hi), E, E, "Compare " + a[j] + " with pivot " + pv);
            if (a[j] < pv) { if (i != j) sw(i, j, rec ? "Swap " + a[i] + " and " + a[j] : null); i++; }
        }
        if (i != hi) sw(i, hi, rec ? "Swap pivot " + pv + " with " + a[i] : null);
        done[i] = true; if (rec) R(E, E, E, "Pivot " + pv + " is in its final position (index " + i + ")");
        qs(lo, i - 1); qs(i + 1, hi);
    }
    private void hp(int m, int i0) {
        P(ph + " – Heapify Step " + (p + 1)); int i = i0;
        while (true) {
            int l = i, x = 2 * i + 1, y = x + 1;
            if (rec) R(E, E, L(i), "Heapify node " + a[i] + " at index " + i);
            if (x < m) { c++; if (rec) R(L(x, l), E, E, "Compare child " + a[x] + " with " + a[l]); if (a[x] > a[l]) l = x; }
            if (y < m) { c++; if (rec) R(L(y, l), E, E, "Compare child " + a[y] + " with " + a[l]); if (a[y] > a[l]) l = y; }
            if (l == i) break;
            sw(i, l, rec ? "Swap " + a[i] + " and " + a[l] : null); i = l;
        }
    }
    private void heap() {
        for (int i = n / 2 - 1; i >= 0; i--) hp(n, i);
        ph = "Extract";
        for (int e = n - 1; e >= 1; e--) { lab = "Extract root"; sw(0, e, rec ? "Move max " + a[0] + " to index " + e : null); done[e] = true; hp(e, 0); }
    }
    private static String join(int[] x) { StringBuilder b = new StringBuilder(); for (int i = 0; i < x.length; i++) { if (i > 0) b.append(','); b.append(x[i]); } return b.toString(); }

    /** Average real run time in ms (no recording). */
    public static double timeIt(String k, int[] d) {
        int reps = Math.max(1, 5000 / d.length); long t0 = System.nanoTime();
        for (int r = 0; r < reps; r++) run(k, d, false);
        return (System.nanoTime() - t0) / 1e6 / reps;
    }

    /** No limit on how many numbers. Items are separated by commas. */
    public static int[] parse(String s) {
        String t = s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Input is empty. Enter numbers separated by commas.");
        String[] parts = t.split(",", -1); int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String q = parts[i].trim();
            try { out[i] = Integer.parseInt(q); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid entry \"" + q + "\" (item " + (i + 1) + "). Use whole numbers separated by commas."); }
        }
        if (out.length < 2) throw new IllegalArgumentException("Enter at least 2 numbers.");
        return out;
    }

    private static boolean h(String m, String re) { return Pattern.compile(re).matcher(m).find(); }

    /** Which pseudocode line (0-based) to highlight for this step, or -1. */
    public static int lineOf(String k, Step st) {
        String m = st.msg;
        switch (k) {
            case "bubble": return h(m, "→ Swap$") ? 4 : h(m, "→ No swap|^Compare") ? 3 : h(m, "^End of pass") ? 5 : -1;
            case "selection": return h(m, "^Assume") ? 1 : h(m, "^Compare|^New minimum") ? 3 : h(m, "^Swap|already the minimum") ? 4 : h(m, "final position") ? 5 : -1;
            case "insertion": return h(m, "^Select") ? 1 : h(m, "^Compare") ? 2 : h(m, "^Shift") ? 3 : h(m, "^Insert") ? 4 : -1;
            case "merge": return h(m, "^Split") ? 2 : h(m, "^Merge") ? 4 : h(m, "^Compare") ? 5 : h(m, "^Place") ? 6 : h(m, "^Copy") ? 7 : -1;
            case "quick": return h(m, "^Pivot =") ? 2 : h(m, "^Pivot") ? 6 : h(m, "^Swap pivot") ? 5 : h(m, "^Swap|^Compare") ? 4 : -1;
            default: return h(m, "^Heapify node") ? 5 : h(m, "^Compare child") ? (st.cmp[0] % 2 == 1 ? 6 : 7) : h(m, "^Move max") ? 2 : h(m, "^Swap") ? 8 : -1;
        }
    }

    /** Beginner-friendly sentence for a step. */
    public static String plain(String m) {
        if (h(m, "^Original")) return "This is your starting list.";
        if (h(m, "No swap")) return "They’re already in the right order, so nothing moves.";
        if (h(m, "^Compare child")) return "We check whether a child is bigger than its parent in the heap.";
        if (h(m, "^Compare")) return "We check which of the highlighted numbers is bigger.";
        if (h(m, "Swap|^Move max")) return "These two are in the wrong order, so they trade places.";
        if (h(m, "^Shift")) return "We slide this number one spot right to make room.";
        if (h(m, "^Insert")) return "The key number drops into its correct spot.";
        if (h(m, "^Select key")) return "We pick up the next number (the key) to place among the sorted ones.";
        if (h(m, "already the minimum")) return "It’s already the smallest, so no swap is needed.";
        if (h(m, "^Assume|^New minimum")) return "We keep track of the smallest number seen so far.";
        if (h(m, "^Pivot =")) return "The pivot (purple) is our reference: smaller numbers go left, bigger go right.";
        if (h(m, "^Pivot")) return "The pivot now sits exactly where it belongs.";
        if (h(m, "final position")) return "This number is now in its final place (green).";
        if (h(m, "^Split")) return "We cut this group in half so each part is easier to sort.";
        if (h(m, "^Merge")) return "We combine two sorted halves into one sorted group.";
        if (h(m, "^Place|^Copy")) return "We write the next smallest number into its spot.";
        if (h(m, "^Heapify")) return "We make sure this number is larger than the two numbers below it in the heap.";
        if (h(m, "Sorted!")) return "All done: every number is in order.";
        return "";
    }
}
