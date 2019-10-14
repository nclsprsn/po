package utils

import groovy.util.logging.Slf4j

@Slf4j
class Utils {

    static final int MAX_LENGTH_HORIZONTAL_BAR = 73

    static List<String> cloneList(List<String> src) {
        List<String> clone = new ArrayList<String>(src.size())
        for (String item : src) {
            clone.add(item)
        }
        clone
    }

    static List<String> diffInDestinationNotInSource(List<String> source, List<String> destination) {
        log.trace(source.toString())
        log.trace(destination.toString())
        // Diff
        def commons = source.intersect(destination)
        def destinationClone = Utils.cloneList(destination)
        destinationClone.removeAll(commons)
        // Result
        destinationClone
    }

    static List<String> diffInSourceNotInDestination(List<String> source, List<String> destination) {
        // Diff
        log.trace(source.toString())
        log.trace(destination.toString())
        def commons = source.intersect(destination)
        def sourceClone = Utils.cloneList(source)
        sourceClone.removeAll(commons)
        // Result
        sourceClone
    }

    static boolean isConfirmed(String sentence) {
        boolean isConfirmed = false

        Utils.log.info ">>> $sentence (y/n)"
        String res = System.in.newReader().readLine()
        if (res == null || res.equalsIgnoreCase('y') || res.equalsIgnoreCase('yes')) {
            isConfirmed = true
        }

        isConfirmed
    }

    static void banner(List<String> content) {
        log.info horizontalBar(MAX_LENGTH_HORIZONTAL_BAR)
        log.info '-'
        content.each {
            log.info '-' + ' ' * ((MAX_LENGTH_HORIZONTAL_BAR - it.length()) / 2 - 1) + it
        }
        log.info '-'
        log.info horizontalBar(MAX_LENGTH_HORIZONTAL_BAR)
    }

    static String horizontalBar(int length) {
        return '-' * length
    }
}
