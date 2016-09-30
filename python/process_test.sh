#!/usr/bin/env bash

path_prefix=$1
index_file=${path_prefix}.indices

sort $index_file > .sorted_correct.indices

echo 'test_name,n,TP,TN,FP,FN'

for out_file in ${path_prefix}*.indices; do
  last_index=`tail -n 1 $out_file` # to make up for the heuristica avoiding the tail of the file
  sort $out_file > .sorted_test.indices

  n=$((last_index-23))
  FN=`comm -23 .sorted_correct.indices .sorted_test.indices | awk -v li=$last_index '$1 <= li' | wc -l | xargs`
  FP=`comm -13 .sorted_correct.indices .sorted_test.indices | wc -l | xargs`
  TP=`comm -12 .sorted_correct.indices .sorted_test.indices | wc -l | xargs`
  TN=$((n-(FN+FP+TP)))

  test_name=`basename ${out_file} .indices`
  echo ${test_name},${n},${TP},${TN},${FP},${FN}

  rm .sorted_test.indices
done

rm .sorted_correct.indices
