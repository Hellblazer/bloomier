/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.hellblazer.bloomier;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

/**
 * @author hal.hildebrand
 *
 */
public abstract class Hash<M> {
    public static class BytesHasher extends Hasher<byte[]> {

        @Override
        protected BytesHasher clone() {
            return new BytesHasher();
        }

        @Override
        protected void processIt(byte[] key) {
            process(key);
        }

    }

    abstract public static class Hasher<M> {

        private static final long C1         = 0x87c37b91114253d5L;
        private static final long C2         = 0x4cf5ad432745937fL;
        private static final long CHUNK_SIZE = 16;

        public static int toInt(byte value) {
            return value & 0xFF;
        }

        long h1;
        long h2;

        int length;

        public Hasher<M> establish(M key, long seed) {
            h1 = seed;
            h2 = seed;
            length = 0;
            processIt(key);
            makeHash();
            return this;
        }

        public long getH1() {
            return h1;
        }

        public int[] hashes(int k, M key, int m, long seed) {
            establish(key, seed);
            long combinedHash = h1;
            int[] hashes = new int[k];
            int i = 0;
            while (i < k) {
                int hash = (int) (((combinedHash) & Long.MAX_VALUE) % m);
                boolean found = false;
                for (int j = 0; j < i; j++) {
                    if (hashes[j] == hash) {
                        found = true;
                        break;
                    } else
                        MISSES++;
                }
                if (!found) {
                    hashes[i++] = hash;
                }
                combinedHash += h2;
            }
            return hashes;
        }

        public int identityHash(M key, long seed) {
            establish(key, seed);
            return (int) (h1 ^ ((h1 >> 32) & Integer.MAX_VALUE));
        }

        public IntStream locations(int k, M key, int m, long seed) {
            return IntStream.of(hashes(k, key, m, seed));
        }

        protected abstract Hasher<M> clone();

        void process(byte[] key) {
            ByteBuffer buff = ByteBuffer.wrap(key);
            while (buff.remaining() >= 16) {
                bmix64(buff.getLong(), buff.getLong());
                length += CHUNK_SIZE;
            }
            if (buff.hasRemaining()) {
                processRemaining(buff);
            }
        }

        void process(int i) {
            long k1 = ((long) (i & 0xFF000000)) << 24;
            k1 ^= ((long) (i & 0x00FF0000)) << 16;
            k1 ^= ((long) (i & 0x0000FF00)) << 8;
            k1 ^= (long) (i & 0x000000FF);

            h1 ^= mixK1(k1);
            h2 ^= mixK2(0);
            length += 4;
        }

        void process(long l) {
            h1 ^= mixK1(l);
            h2 ^= mixK2(0);
            length += 8;
        }

        void process(String key) {
            process(key.getBytes());
        }

        abstract void processIt(M key);

        private void bmix64(long k1, long k2) {
            h1 ^= mixK1(k1);

            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            h2 ^= mixK2(k2);

            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        private long fmix64(long k) {
            k ^= k >>> 33;
            k *= 0xff51afd7ed558ccdL;
            k ^= k >>> 33;
            k *= 0xc4ceb9fe1a85ec53L;
            k ^= k >>> 33;
            return k;
        }

        private void makeHash() {
            h1 ^= length;
            h2 ^= length;

            h1 += h2;
            h2 += h1;

            h1 = fmix64(h1);
            h2 = fmix64(h2);

            h1 += h2;
            h2 += h1;
        }

        private long mixK1(long k1) {
            k1 *= C1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= C2;
            return k1;
        }

        private long mixK2(long k2) {
            k2 *= C2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= C1;
            return k2;
        }

        private void processRemaining(ByteBuffer buff) {
            long k1 = 0;
            long k2 = 0;
            length += buff.remaining();
            switch (buff.remaining()) {
            case 15:
                k2 ^= (long) toInt(buff.get(14)) << 48; // fall through
            case 14:
                k2 ^= (long) toInt(buff.get(13)) << 40; // fall through
            case 13:
                k2 ^= (long) toInt(buff.get(12)) << 32; // fall through
            case 12:
                k2 ^= (long) toInt(buff.get(11)) << 24; // fall through
            case 11:
                k2 ^= (long) toInt(buff.get(10)) << 16; // fall through
            case 10:
                k2 ^= (long) toInt(buff.get(9)) << 8; // fall through
            case 9:
                k2 ^= (long) toInt(buff.get(8)); // fall through
            case 8:
                k1 ^= buff.getLong();
                break;
            case 7:
                k1 ^= (long) toInt(buff.get(6)) << 48; // fall through
            case 6:
                k1 ^= (long) toInt(buff.get(5)) << 40; // fall through
            case 5:
                k1 ^= (long) toInt(buff.get(4)) << 32; // fall through
            case 4:
                k1 ^= (long) toInt(buff.get(3)) << 24; // fall through
            case 3:
                k1 ^= (long) toInt(buff.get(2)) << 16; // fall through
            case 2:
                k1 ^= (long) toInt(buff.get(1)) << 8; // fall through
            case 1:
                k1 ^= (long) toInt(buff.get(0));
                break;
            default:
                throw new AssertionError("Should never get here.");
            }
            h1 ^= mixK1(k1);
            h2 ^= mixK2(k2);
        }
    }

    public static class IntHasher extends Hasher<Integer> {
        @Override
        protected Hasher<Integer> clone() {
            return new IntHasher();
        }

        @Override
        protected void processIt(Integer key) {
            process(key);
        }

    }

    public static class LongHasher extends Hasher<Long> {

        @Override
        protected Hasher<Long> clone() {
            return new LongHasher();
        }

        @Override
        protected void processIt(Long key) {
            process(key);
        }

    }

    public static class StringHasher extends Hasher<String> {

        @Override
        protected StringHasher clone() {
            return new StringHasher();
        }

        @Override
        protected void processIt(String key) {
            process(key);
        }

    }

    public static int MISSES = 0;

    /**
     * Computes the optimal k (number of hashes per element inserted in Bloom
     * filter), given the expected insertions and total number of bits in the Bloom
     * filter.
     *
     * See http://en.wikipedia.org/wiki/File:Bloom_filter_fp_probability.svg for the
     * formula.
     *
     * @param n expected insertions (must be positive)
     * @param m total number of bits in Bloom filter (must be positive)
     */
    public static int optimalK(long n, long m) {
        // (m / n) * log(2), but avoid truncation due to division!
        return Math.max(1, (int) Math.round(((double) m) / ((double) n) * Math.log(2)));
    }

    /**
     * Computes m (total bits of Bloom filter) which is expected to achieve, for the
     * specified expected insertions, the required false positive probability.
     *
     * See http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives
     * for the formula.
     *
     * @param n expected insertions (must be positive)
     * @param p false positive rate (must be 0 < p < 1)
     */
    public static int optimalM(long n, double p) {
        if (p == 0) {
            p = Double.MIN_VALUE;
        }
        return Math.max(8, (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2))));
    }

    protected final Hasher<M> hasher;
    protected final int       k;
    protected final int       m;
    protected final long      seed;

    public Hash(long seed, int n, double p) {
        m = optimalM(n, p);
        k = optimalK(n, m);
        this.seed = seed;
        hasher = newHasher();
    }

    public Hash(long seed, int k, int m) {
        this.seed = seed;
        this.k = k;
        this.m = m;
        hasher = newHasher();
    }

    public Hash<M> clone() {
        Hasher<M> clone = hasher.clone();
        return new Hash<M>(seed, k, m) {

            @Override
            Hasher<M> newHasher() {
                return clone;
            }
        };
    }

    public int getK() {
        return k;
    }

    public int getM() {
        return m;
    }

    public long getSeed() {
        return seed;
    }

    public int[] hashes(M key) {
        return hasher.hashes(k, key, m, seed);
    }

    public int identityHash(M key) {
        return hasher.identityHash(key, seed);
    }

    public IntStream locations(M key) {
        return hasher.locations(k, key, m, seed);
    }

    abstract Hasher<M> newHasher();
}
