#!/usr/bin/perl
# Begin strip_html_markup.pl
######## 
#$# creator : Devon Smith
#%# email: smithde@oclc.org
#$# created : 2010-05-11
#$# title : strip_html_markup.pl
#$# description : Extract NER-taggable text from HTML files
######## Define a usage function 
sub usage {
	(my $bin = $0) =~ s,^.*/,,;
	print "Usage:\t$bin html-file text-file\n";
	exit if shift eq 'exit';
}
######## Standard use lines
use strict;
use warnings;
use FileHandle;
use HTML::Strip;
######## 
&usage('exit') if $#ARGV < 1;

my $html_file = shift @ARGV;
my $text_file = shift @ARGV;

my $input = FileHandle->new(); 
$input->open($html_file, 'r') or die "Can't open \"$html_file\":$!";

my $output = FileHandle->new(); 
$output->open("$text_file", 'w') or die "Can't open \"$text_file\":$!";

{
	my $p = HTML::Strip->new();
	local $/;
	print $output $p->parse(<$input>);
}

$input->close();
$output->close();

######## 
# End strip_html_markup.pl
# vim:ts=4:indentexpr=
