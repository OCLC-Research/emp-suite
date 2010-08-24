#!/usr/bin/perl
# Begin crop.pl
######## 
#$# creator : Devon Smith
#%# email: smithde@oclc.org
#$# created : 2010-06-02
#$# title : crop.pl
#$# description : Crop a file 
#__# Use a regular expression to find the start of a 'section' in a file,
#__# and another regular expression to find the end of the section, 
#__# You can check that your regular expressions match as
#__# expected with grep before cropping the file.
#__# NB: You can use the csplit utility for this if you have it.
######## Define a usage function 
sub usage {
	(my $bin = $0) =~ s,^.*/,,;
	print "Usage:\t$bin input-file start-pattern end-pattern output-file\n";
	exit if shift eq exit;
}
######## Standard use lines
use strict;
use warnings;
use FileHandle;
######## 
&usage('exit') if $#ARGV < 3;

my $in_file = shift @ARGV;
my $s = shift @ARGV;
my $e = shift @ARGV || $s;
my $out_file = shift @ARGV;

my $input = FileHandle->new(); 
$input->open($in_file, 'r') or die "Can't open \"$in_file\":$!";

my $output = FileHandle->new(); 
$output->open($out_file, 'w') or die "Can't open \"$out_file\":$!";

my $start_pattern = qr($s);

my $end_pattern = qr($e);

my $section = 0;
while (<$input>) {
	$section = 1 if m/$start_pattern/;
	print $output $_ if $section;
	last if m/$end_pattern/;
}

$input->close();
$output->close();

######## 
# End crop.pl
# vim:ts=4:indentexpr=
