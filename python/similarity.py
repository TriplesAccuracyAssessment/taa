#
# (C) Copyright 2017 Shuangyan Liu
# Shuangyan.Liu@open.ac.uk 
# Knowledge Media Institute
# The Open University, United Kingdom
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#  
#   http://www.apache.org/licenses/LICENSE-2.0
#  
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#  
#
#Predicate semantic similarity in Python2
import numpy
import json
import sys
from sematch.semantic.similarity import WordNetSimilarity
from nltk.wsd import lesk
from nltk.corpus import wordnet_ic
from nltk.corpus.reader.wordnet import information_content

brown_ic = wordnet_ic.ic('ic-brown.dat')
wns = WordNetSimilarity()
# arg1 and arg2: predicates represented in strings separated by underscores
# e.g. cast_member or star 
preA = sys.argv[1].split("_")
preB = sys.argv[2].split("_")
# arg3: pairwise similarity matrix in which rows are separated by underscore
# e.g. 0.6_0.5, or 0.6,0.7_0.3,0.4
data = []
for a in preA:
    row = []
    for b in preB:
        wdsim = wns.word_similarity(a, b, 'wup')
        row.append(wdsim)
    data.append(row)
data = numpy.matrix(data)
#max values in rows
Amax = data.max(1)
icA = []
for i in range(len(preA)):
    try:    
        if lesk(preA,preA[i]) is None:
            # preA is not in WordNet
            icA.append(1)
        elif information_content(lesk(preA, preA[i]), brown_ic) == 'inf': 
            icA.append(1)
        else:
            icA.append(information_content(lesk(preA, preA[i]), brown_ic))
    except Exception:
        icA.append(1)            
icA = numpy.matrix(icA)
#max values in columns
Bmax = data.max(0)
icB = []
for i in range(len(preB)):
    try:
        if lesk(preB, preB[i]) is None:
            # preB is not in WordNet
            icB.append(1)
        elif information_content(lesk(preB, preB[i]), brown_ic) == 'inf':
            icB.append(1)
        else:
            icB.append(information_content(lesk(preB, preB[i]), brown_ic))
    except Exception:
        icB.append(1)
# predicate semantic similarity
similarity = 0.5 * (icA.dot(Amax)/numpy.sum(icA) + Bmax.dot(icB)/numpy.sum(icB))
print similarity.item(0)

