#!/usr/bin/python
# -*- coding: iso-8859-1 -*-
###############################################################################
# To parse the results of Named Entity Recognizer(NER) annotators/tools       #
###############################################################################
# Date: 8/17/09                                                               #
# Author: the EMP project group in Univ. of Maryland (hcho5@cs.umd.edu)       #
###############################################################################
# Date-Modified: 2010-04-07                                                   #
# Modifier: OCLC Research (smithde@oclc.org)                                  #
###############################################################################
import re
import sys
import string

from codecs import open

import subprocess

from jinja2 import Template, Environment, FileSystemLoader
from yaml import load, dump
try:
    from yaml import CLoader as Loader
    from yaml import CDumper as Dumper
except ImportError:
    from yaml import Loader, Dumper

verbose = False
def echo(str):
    if verbose:
        print str

# Encapsulate yaml emitting with preferred options
# Write YAML to a file
def serialize(filename, data):
    yaml_file = open(filename, 'w')
    dump(data, yaml_file, default_flow_style=False, width=250)
    yaml_file.close()

# Encapsulate yaml emitting with preferred options
# Write YAML to stdout (pump = print dump)
def pump(data):
    print dump(data, default_flow_style=False, width=250)


# Default precision to 1? is that reasonable if tp is zero?
def precision(tp, fp):
    precision=1
    if 0 != tp + fp:
        precision = 100 * ( tp / float(tp + fp) )
    return precision

# Default recall to 1?
def recall(tp, fn):
    recall=1
    if 0 != tp + fn:
        recall = 100 * ( tp / float(tp + fn) )
    return recall

# Default f-measure to 1?
def fmeasure(p, r):
    fmeasure=1
    if 0 != p + r:
        fmeasure = (2 * p * r) / (p + r)
    return fmeasure

def metric_calculation(metric):
    metric['precision'] = precision(metric['truepos'], metric['falsepos'])
    metric['recall'] = recall(metric['truepos'], metric['falseneg'])
    metric['fmeasure'] = fmeasure(metric['precision'], metric['recall'])

    metric['partprecision'] = precision(metric['parttruepos'], metric['partfalsepos'])
    metric['partrecall'] = recall(metric['parttruepos'], metric['falseneg'])
    metric['partfmeasure'] = fmeasure(metric['partprecision'], metric['partrecall'])

def hash_roll_in(dest, source):
    for key in source:
        dest[key] = dest[key] + source[key] if key in dest else source[key]

def append_lists(f_lst, s_lst, size):
    res_lst = create_lists(size)
    for idx in range(len(f_lst)):
        res_lst[idx] = u'' + f_lst[idx] + s_lst[idx]
    return res_lst

def create_lists(num):
    list = []
    for i in range(num):
        list.append(u'')
    return list

def empty_res_list(lst):
    isEmpty = True
    for ele in lst:
        if ele is not u'':
            isEmpty = False
    return isEmpty

# append the value of each source key to 
def append_dictionary(target, source):
    dest = init_dictionary(target.keys())
    for key in target:
        dest[key] = u'' + target[key] + source[key]
    return dest

def init_dictionary(keys):
    dictionary = {}
    for k in keys:
        dictionary[k] = u''
    return dictionary

def empty_res_dic(dic):
    for key in dic:
        if dic[key] is not u'':
            return False
    return True

def init_metric():
    metric_keys = [ 'truepos', 'falsepos', 'trueneg', 'falseneg',
                    'parttruepos', 'partfalsepos', 'label' ]
    return dict.fromkeys(metric_keys, 0)

# Check if the word contains a capital letter
# if contains cap(s), return True
# o.w., return False
def check_caps(word):
    for i in xrange(len(word)):
        if word[i].isupper():
            return True
    return False

def extract_tag(string):
    start = string.find('[') + 1
    end = string.find(' ', start)
    return string[start:end]

# If the line contains a particular ClearForest string label,
# write a corresponding NER tag based on mapping dictionary
def cf_write_line(tag, line, corpus, tag_index):
    p_layer = re.compile(r'(\<layer[^\>]*\>)')
    p_layer2 = re.compile(r'(\<\/layer\>)')

    cf_mapping = {"Person":"PER", "Position":"PER",
                  "City":"LOC", "ProvinceOrState":"LOC", "Country":"LOC",
                  "Region":"LOC", "Continent":"LOC", "NaturalFeature":"LOC",
                  "Organization":"ORG", "Company":"ORG", "Facility":"ORG",
                  "IndustryTerm":"MISC"}

    # Mapping dictionary for replacing ClearForest labels with regular NER taggs
    res_line = ""

    if line.find('id="'+tag+'">') == 0:
        temp_line = line

        for p1 in p_layer.split(temp_line):
            if p1.find("<layer") is not 0:
                for p2 in p_layer2.split(p1):
                    if p2.strip() == "":
                        continue
                    if p2.find("</layer") is not 0:
                        p_layer3 = re.compile(r'(\<\/\w*)')
                        for p3 in p_layer3.split(p2):
                            if p3.find("</") is not 0: 
                                res_line = res_line + p3


        res_line = res_line[len('id="'+tag+'">'):(len(temp_line)-1)]
        res_line = res_line[0:30].strip()

        offset = corpus['original'].find(''.join(res_line.split()))
        bracketed_tag = "["+cf_mapping[tag]+ " " + line[len('id="'+tag+'">'):line.find("<")] + "]"

        if ((tag == "Position") & (check_caps(line[len('id="Position">'):line.find("<")]) is not True)) | ((tag == "IndustryTerm") & (check_caps(line[len('id="IndustryTerm">'):line.find("<")]) is not True)):
            return
        else:
            tag_index[offset] = bracketed_tag


def find_cf_tags(tagged_text, tag, corpus, tag_index):
    s_idx = 0
    line = ""
    tag = str[len('id="'):len(tag)-len('">')]
    while s_idx >= 0:
        s_idx = tagged_text.find(tag, s_idx)
        if s_idx >= 0:
            line = tagged_text[s_idx:s_idx+300]
            cf_write_line(tag, line, corpus, tag_index)
            s_idx = s_idx + 1


# parser for ClearForest Gnosis
def clearforest_parser(filename, corpus, model):
    file = open(filename, 'r', 'latin-1')
    lines = file.readlines()
    file.close()

    tagged_text = " ".join(map(lambda x: x.strip(), lines))

    cf_tags = [ 'id="Position">', 'id="Person">', 'id="Organization">',
                'id="Company">', 'id="Facility">', 'id="Continent">',
                'id="Country">', 'id="Region">', 'id="ProvinceOrState">',
                'id="City">', 'id="NaturalFeature">', 'id="IndustryTerm">']

    # write an NER tag in order in output file
    index = {}
    for tag in cf_tags:
        find_cf_tags(tagged_text, tag, corpus, index)
    return index

# write a NER tag for Stanford NER and UIUC NER model(LBJ)                                                                    
def write_line(line, corpus, tag_index):
    # remove a space at the end when using UIUC NER model
    line = line.replace(u" ]", u"]")
    res_line = line[0:line.find("]")+1]

    # Remove tags from the line
    for tag in config['setup']['tags']:
        tag_in_situ = re.sub('\${tag}', tag, config['setup']['pattern'])
        line = line.replace(tag_in_situ, "")
    line = line.replace(u"]", u"")

    # Find the offset of the start of the tag in the untagged file
    line = line[0:25].strip()
    offset = corpus['original'].find(u''.join(line.split()))

    tag_index[offset] = res_line

def increment_tag_frequency(tag, corpus, model):
    model = model['label']
    if model not in corpus['frequency']:
        corpus['frequency'][model]= {}

    if tag in corpus['frequency'][model]:
        corpus['frequency'][model][tag] += 1
    else:
        corpus['frequency'][model][tag] = 1

def find_tags(tagged_text, tag, corpus, model, tag_index):
    s_idx = 0
    line = u''
    tag_in_situ = re.sub('\${tag}', tag, config['setup']['pattern'])
    while s_idx >= 0:
        #s_idx = tagged_text.find(tag, s_idx)
        s_idx = tagged_text.find(tag_in_situ, s_idx)
        if s_idx >= 0:
            # What should this 250 be? Need to determine its real use and pick a value with that knowledge
            line = tagged_text[s_idx:s_idx+50]
            increment_tag_frequency(tag, corpus, model)
            write_line(line, corpus, tag_index)
            s_idx = s_idx + 1

# Parse CONLL tagged files
def bracket_parser(filename, corpus, model):
    file = open(filename, 'r', 'latin1')
    lines = file.readlines()
    file.close()

    tagged_text = u' '.join(map(lambda x: x.strip(), lines))

    # For non-bracket formats where simple substitutions convert to bracket
    if "linetransform" in model:
        for (k,v) in model["linetransform"].iteritems():
            tagged_text = tagged_text.replace(k,v)

    index = {}
    for tag in config['setup']['tags']:
        find_tags(tagged_text, tag, corpus, model, index)
    return index

def parse_tagged_input(model, corpus):
    filename = "data/%s/%s.txt" % (corpus['label'], model['prefix'])
    echo("Parsing %s" % model['label'])
    index = {}
    if model['parser'] == 'clearforest':
        index = clearforest_parser(filename, corpus, model)
    elif model['parser'] == 'bracket':
        index = bracket_parser(filename, corpus, model)

    return index


def collate_tags(corpus):
    keylist = []
    for model in corpus['tagged']:
        keylist.extend(corpus['tagged'][model].keys())
    # dedup the keylist
    keylist = set(keylist)

    collation = {}
    result_dic = {}

    # Prepare the inverted data structure: inverting [model][offset] into [offset][model]
    for key in keylist:
        result_dic[key] = {}
        for model in corpus['tagged']:
            result_dic[key][model] = corpus['tagged'][model][key] if key in corpus['tagged'][model] else u''


    offsets = result_dic.keys()
    offsets.sort()

    prev_key = 0
    highest_end_idx = 0
    #res_list = create_lists(len(indexed_tags))
    res_dic = init_dictionary(corpus['tagged'])
    res_key = 0

    res_lists = []

    cnt = 0
    for key in offsets:
        length = 0

        for model in corpus['tagged']:
            adj_len = 7 if result_dic[key][model].find("[MISC") != 0 else 6
            if (len(result_dic[key][model]) - adj_len) >= length:
                length = len(result_dic[key][model].replace(u' ', u'')) - adj_len

        if highest_end_idx == 0:
            res_key = key
            res_dic = result_dic[key]
            highest_end_idx = key + length
        elif (highest_end_idx) >= key:
            res_dic = append_dictionary(res_dic, result_dic[key])
        else:
            if empty_res_dic(res_dic) is not True:
                cnt = cnt + 1
                echo("%s: %s %s %s" % (cnt, res_key, res_dic, highest_end_idx))
                collation[res_key] = res_dic

            res_key = key
            res_dic = result_dic[key]
            highest_end_idx = (key + length)

    cnt = cnt + 1
    echo("%s: %s %s %s" % (cnt, res_key, res_dic, highest_end_idx))
    collation[res_key] = res_dic
    corpus['collated'] = collation

def score_corpus(corpus):
    gold = corpus['tagged']['gold']
    gold_keylist = gold.keys()
    gold_keylist.sort()

    offsets = corpus['collated'].keys()
    offsets.sort()

    collation = corpus['collated']

    for model_key in config['models']:
        if model_key == 'gold':
            continue
        model = config['models'][model_key]
        cnt = 0

        model_corpus_metrics = {}

        for key in offsets:
#            if (gold_keylist[0] > key) | (gold_keylist[-1] < key):
#                # this should be a FP, yes?
#                continue

            # move the text to sub key 
            collation[key][model_key] = { 'text': collation[key][model_key], 'metric': {} }

            gold_text = collation[key]['gold']
            gold_tag = extract_tag(gold_text)

            model_text = collation[key][model_key]['text']
            model_tag = extract_tag(model_text)

            tag_metric = init_metric()
            tag_metric['label'] = gold_tag # use gold tag by default

            if (gold_text == "") & (model_text == ""):
                tag_metric['trueneg'] = 1
                # need tag_metric['label'] must come from a different model .... 
                for fn in collation[key]:
                    if fn == 'gold' or fn == model_key:
                        continue
                    test_label = extract_tag(collation[key][fn]) if 'text' not in collation[key][fn] else extract_tag(collation[key][fn]['text'])
                    if test_label != "":
                        tag_metric['label'] = test_label
            elif (gold_text is not "") & (model_text == ""):
                tag_metric['falseneg'] = 1
            else:
                if config['setup']['compare'] == 'space-insensitive':
                    translate_table = dict((ord(char), None) for char in string.whitespace)
                    # Compare with spaces deleted
                    gold_cmp = gold_text.translate(translate_table)
                    model_cmp = model_text.translate(translate_table)
                else:
                    gold_cmp = gold_text
                    model_cmp = model_text

                if gold_cmp == model_cmp:
                    tag_metric['truepos'] = 1
                else:
                    tag_metric['label'] = gold_tag if gold_tag != '' else model_tag
                    tag_metric['falsepos'] = 1
                if gold_text.find(model_text[0:5]) == 0:
                    tag_metric['parttruepos'] = 1
                else:
                    tag_metric['label'] = gold_tag if gold_tag != '' else model_tag
                    tag_metric['partfalsepos'] = 1

            collation[key][model_key]['metric'] = tag_metric;
            metric_roll_in(model_corpus_metrics, tag_metric.copy())

        model['score']['corpora'][corpus['label']]['frequency'] = corpus['frequency'][ model['label'] ] 
        model['score']['corpora'][corpus['label']]['tags'] = model_corpus_metrics
        model['score']['corpora'][corpus['label']]['summary'] = summarize(model_corpus_metrics)

def metric_roll_in(base, source):
    tag = source.pop('label')
    if tag not in base:
        base[tag] = source
    else:
        for m in source:
            base[tag][m] += source[m]


def summarize(tags):
    summary = {}
    for tag in tags:
        hash_roll_in(summary, tags[tag])
    return summary

def apply_template(template, data, filename):
    env = Environment(loader=FileSystemLoader('jt'))
    output_file = open(filename, 'w', 'utf-8')
    template = env.get_template(template)
    output_file.write(template.render(data))
    output_file.close()

def generate_corpus_report(corpus):
    apply_template('corpus.html', {'corpus': corpus, 'config': config}, "ht/%s.html" % corpus['label'])

def generate_model_report(model, key):
    apply_template('model.html', {'model': model, 'config': config, 'key': key}, "ht/%s.html" % key)

def generate_graph_data(model, config, key):
    apply_template('graph.csv', {'model': model, 'config': config}, "data/%s.csv" % key)
    try:
        # usage: plot_metrics data-file png-file graph-title
        subprocess.call(["./plot_metrics", "data/%s.csv" % key, "ht/%s.png" % key, "%s / %s" % (config['html']['title'], config['models'][key]['label']) ])
    except OSError:
        print "Skipping graphs for %s" % tool['label']

def generate_index(config):
    apply_template('index.html', {'config': config}, "ht/index.html")

def load_original(name):
    filename = "data/%s/original.txt" % name
    text_file = open(filename, 'r', 'latin-1')
    lines = text_file.readlines()
    text_file.close()
    return ''.join(map(lambda x: ''.join(x.split()), lines))

def labelize(name):
    # make all caps
    name = u'' + name.upper()
    # Replace punctuation with <space>
    translate_table = dict((ord(char), u' ') for char in string.punctuation)
    return name.translate(translate_table)

######## MAIN ########

config = load(file('config.yml', 'r'))
if 'verbose' in config['setup']:
    verbose = config['setup']['verbose']

for name in config['corpora']:
    corpus = { 'tagged': {}, 'frequency': {}, 'label': name, 'display_label': labelize(name), 'original': load_original(name) }

    corpus['tagged']['gold'] = parse_tagged_input(config['gold'], corpus)

    for key in config['models']:
        model = config['models'][key]
        model['score']['corpora'][name] = { 'display': labelize(name), 'summary': {}, 'tags': {}}
        # Read the tagged input in the (model x corpus) matrix
        corpus['tagged'][ key ] = parse_tagged_input(model, corpus)

    collate_tags(corpus)
    score_corpus(corpus)
    del corpus['original']
    #pump(corpus)
    generate_corpus_report(corpus)


for key in config['models']:
    model = config['models'][key]

    for name in config['corpora']:
        # Roll in the data for each corpus into summary and frequency
        hash_roll_in(model['score']['summary'], model['score']['corpora'][name]['summary']) 
        hash_roll_in(model['score']['frequency'], model['score']['corpora'][name]['frequency']) 

        # Calculate precision, recall, and f-measure for the file-summary
        metric_calculation(model['score']['corpora'][name]['summary'])

        # Roll in the tag-based counts for each file
        for tag in config['setup']['tags']:
            if tag not in model['score']['corpora'][name]['tags']:
                continue
            if tag not in model['score']['tags']:
                model['score']['tags'][tag] = {}
            hash_roll_in(model['score']['tags'][tag], model['score']['corpora'][name]['tags'][tag]) 

    # Calculate precision, recall, and f-measure for each tag
    for tag in config['setup']['tags']:
        metric_calculation(model['score']['tags'][tag])

    # Calculate precision, recall, and f-measure for the model summary
    metric_calculation(model['score']['summary'])
    #pump(model)
    serialize('data/%s_score.yml' % key, model)
    generate_model_report(model, key)
    generate_graph_data(model, config, key)

generate_index(config)

# END score.py
# vim: expandtab:ts=4:list
