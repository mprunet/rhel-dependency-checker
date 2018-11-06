package eu.prunet.security.rhelchecker.rpm;

class RpmUtils {

    private static int strcmp(CharPointer one, CharPointer two) {
        return one.toString().compareTo(two.toString());
    }

    /* compare alpha and numeric segments of two versions */
    /* return 1: a is newer than b */
    /*        0: a and b are the same version */
    /*       -1: b is newer than a */
    private static int rpmvercmp(String a, String b) {
        /* easy comparison to see if versions are identical */
        if (a.equals(b)) return 0;

        char oldch1, oldch2;
        int rc;
        boolean isnum;

        CharPointer str1;
        CharPointer str2;
        CharPointer one = new CharPointer(a);
        CharPointer two = new CharPointer(b);

        /* loop through each version segment of str1 and str2 and compare them */
        while (one.nEof() || two.nEof()) {
            while (one.nEof() && !one.risalnum() && one.not('~')) one.inc();
            while (two.nEof() && !two.risalnum() && one.not('~')) two.inc();

            /* handle the tilde separator, it sorts before everything else */
            if (one.is('~') || two.is('~')) {
                if (one.not('~')) return 1;
                if (two.not('~')) return -1;
                one.inc();
                two.inc();
                continue;
            }

            /* If we ran to the end of either, we are finished with the loop */
            if (!(one.nEof() && two.nEof())) break;

            str1 = new CharPointer(one);
            str2 = new CharPointer(two);

            /* grab first completely alpha or completely numeric segment */
            /* leave one and two pointing to the start of the alpha or numeric */
            /* segment and walk str1 and str2 to end of segment */
            if (str1.risdigit()) {
                while (str1.nEof() && str1.risdigit()) str1.inc();
                while (str2.nEof() && str2.risdigit()) str2.inc();
                isnum = true;
            } else {
                while (str1.nEof() && str1.risalpha()) str1.inc();
                while (str2.nEof() && str2.risalpha()) str2.inc();
                isnum = false;
            }

            /* save character at the end of the alpha or numeric segment */
            /* so that they can be restored after the comparison */
            oldch1 = str1.get();
            str1.set('\0');
            oldch2 = str2.get();
            str2.set('\0');

            /* this cannot happen, as we previously tested to make sure that */
            /* the first string has a non-null segment */
            if (one.get() == str1.get()) return -1;    /* arbitrary */

            /* take care of the case where the two version segments are */
            /* different types: one numeric, the other alpha (i.e. empty) */
            /* numeric segments are always newer than alpha segments */
            /* XXX See patch #60884 (and details) from bugzilla #50977. */
            if (two.get() == str2.get()) return (isnum ? 1 : -1);

            if (isnum) {
                int onelen, twolen;
                /* this used to be done by converting the digit segments */
                /* to ints using atoi() - it's changed because long  */
                /* digit segments can overflow an int - this should fix that. */

                /* throw away any leading zeros - it's a number, right? */
                while (one.get() == '0') one.inc();
                while (two.get() == '0') two.inc();

                /* whichever number has more digits wins */
                onelen = one.strlen();
                twolen = two.strlen();
                if (onelen > twolen) return 1;
                if (twolen > onelen) return -1;
            }

            /* strcmp will return which one is greater - even if the two */
            /* segments are alpha or if they are numeric.  don't return  */
            /* if they are equal because there might be more segments to */
            /* compare */
            rc = strcmp(one, two);
            if (rc != 0) return (rc < 1 ? -1 : 1);

            /* restore character that was replaced by null above */
            str1.set(oldch1);
            one = str1;
            str2.set(oldch2);
            two = str2;
        }

        /* this catches the case where all numeric and alpha segments have */
        /* compared identically but the segment sepparating characters were */
        /* different */
        if ((one.nEof()) && (two.nEof())) return 0;

        /* whichever version still has characters left over wins */
        if (one.eof()) return -1;
        else return 1;
    }



    public static int compare(long epochOne, String versionOne, String releaseOne, String evr) {
        String[] sevr = evr.split(":|\\-");
        if (sevr.length != 3) {
            throw new UnsupportedOperationException("EVR cannot be parsed " +evr + " "+sevr.length);
        }

        long epochTwo = Long.parseLong(sevr[0]);
        if (epochOne < epochTwo) {
            return -1;
        } else if (epochOne > epochTwo) {
            return 1;
        }
        int ret;
        String versionTwo = sevr[1];
        ret = rpmvercmp(versionOne, versionTwo);
        if (ret == 0) {
            String releaseTwo = sevr[2];
            ret = rpmvercmp(releaseOne, releaseTwo);
        }
        return ret;
    }
}
