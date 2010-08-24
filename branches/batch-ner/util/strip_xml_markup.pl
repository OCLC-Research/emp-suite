#!/usr/bin/perl
# Begin strip_xml_markup.pl
######## 
#$# creator : Devon Smith
#%# email: smithde@oclc.org
#$# created : 2010-05-11
#$# title : strip_xml_markup.pl
#$# description : Extract text from XML files
######## Define a usage function 
sub usage {
	(my $bin = $0) =~ s,^.*/,,;
	print "Usage:\t$bin xml-file text-file [ --compress-whitespace ]\n";
	exit if shift eq exit;
}
######## Standard use lines
use strict;
use warnings;
use FileHandle;
use FindBin qw($Bin);
use lib "$Bin/../lib";
use XML::Parser;
#  ../lib/XML/Parser/Style/Switch.pm
######## 
&usage('exit') if $#ARGV < 1;

# Default is to extract all CData from the file.
# Modify this list with paths to extract CData from a limited set of elements
# eg, my @tags = ( 'ead/archdesc/did/abstract','ead/archdesc/scopecontent');
my @tags = ( '#root' ) ;

my $xml_file = shift @ARGV;
my $text_file = shift @ARGV;
my $ws_compress = $ARGV[0] && $ARGV[0] eq '--compress-whitespace' ? 1 : 0;

my $raw = FileHandle->new(); 
$raw->open($xml_file, 'r') or die "Can't open \"$xml_file\":$!";

my $dest = FileHandle->new(); 
$dest->open($text_file, 'w') or die "Can't open \"$text_file\":$!";

my $p = new XML::Parser(Style => 'Switch', Pkg => 'CDataExtract', DataTags => \@tags );

{
	my $text = $p->parse($raw);
	# to compress all whitespace into single space
	$text =~ s/\s+/ /g if $ws_compress;
	print $dest $text;
}
	
$raw->close();
$dest->close();

package CDataExtract;
our (@DataTags, $DataVar);

sub StartDocument {
	my $expat = shift;
	push @DataTags, @{$expat->{DataTags}} if $expat->{DataTags};
}

sub EndDocument {
	return $DataVar;
}

1;
######### 
## End strip_xml_markup.pl
## vim:ts=4:indentexpr=
