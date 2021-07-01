/*
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.hellblazer.bloomier;

import java.util.BitSet;

/**
 * Simplified Bloom filter for multiple types, with setable seeds and other
 * parameters.
 * 
 * @author hal.hildebrand
 *
 */
abstract public class BloomFilter<T> {
    public static class BytesBloomFilter extends BloomFilter<byte[]> {

        public BytesBloomFilter(long seed, int n, double p) {
            super(new Hash<byte[]>(seed, n, p) {
                @Override
                Hasher<byte[]> newHasher() {
                    return new BytesHasher();
                }
            });
        }

        public BytesBloomFilter(long seed, int m, int k, long[] bytes) {
            super(new Hash<byte[]>(seed, k, m) {
                @Override
                Hasher<byte[]> newHasher() {
                    return new BytesHasher();
                }
            }, BitSet.valueOf(bytes));
        }
    }

    public static class IntBloomFilter extends BloomFilter<Integer> {

        public IntBloomFilter(long seed, int n, double p) {
            super(new Hash<Integer>(seed, n, p) {
                @Override
                Hasher<Integer> newHasher() {
                    return new IntHasher();
                }
            });
        }

        public IntBloomFilter(long seed, int m, int k, long[] bits) {
            super(new Hash<Integer>(seed, k, m) {
                @Override
                Hasher<Integer> newHasher() {
                    return new IntHasher();
                }
            }, BitSet.valueOf(bits));
        }
    }

    public static class LongBloomFilter extends BloomFilter<Long> {
        public LongBloomFilter(long seed, int n, double p) {
            super(new Hash<Long>(seed, n, p) {
                @Override
                Hasher<Long> newHasher() {
                    return new LongHasher();
                }
            });
        }

        public LongBloomFilter(long seed, int m, int k, long[] bits) {
            super(new Hash<Long>(seed, k, m) {
                @Override
                Hasher<Long> newHasher() {
                    return new LongHasher();
                }
            }, BitSet.valueOf(bits));
        }
    }

    public static class StringBloomFilter extends BloomFilter<String> {

        public StringBloomFilter(long seed, int n, double p) {
            super(new Hash<String>(seed, n, p) {
                @Override
                Hasher<String> newHasher() {
                    return new StringHasher();
                }
            });
        }

        public StringBloomFilter(long seed, int m, int k, long[] bytes) {
            super(new Hash<String>(seed, k, m) {
                @Override
                Hasher<String> newHasher() {
                    return new StringHasher();
                }
            }, BitSet.valueOf(bytes));
        }
    }

    public static double population(BitSet bitSet, int k, int m) {
        int oneBits = bitSet.cardinality();
        return -m / ((double) k) * Math.log(1 - oneBits / ((double) m));
    }

    private final BitSet  bits;
    private final Hash<T> h;

    private BloomFilter(Hash<T> h) {
        this(h, new BitSet(h.getM()));
    }

    private BloomFilter(Hash<T> h, BitSet bits) {
        this.h = h;
        this.bits = bits;
    }

    public void add(T element) {
        for (int hash : h.hashes(element)) {
            bits.set(hash);
        }
    }

    public void clear() {
        bits.clear();
    }

    public boolean contains(T element) {
        for (int hash : h.hashes(element)) {
            if (!bits.get(hash)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Estimates the current population of the Bloom filter (see:
     * http://en.wikipedia.org/wiki/Bloom_filter#Approximating_the_number_of_items_in_a_Bloom_filter
     *
     * @return the estimated amount of elements in the filter
     */
    public double getEstimatedPopulation() {
        return population(bits, h.getK(), h.getM());
    }
}
