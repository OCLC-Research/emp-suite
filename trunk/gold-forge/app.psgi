#!/usr/bin/perl
#
#$# creator : Devon Smith
#$# created : 2009-11-18
#$# title : 
#$# description : 
## Standard use lines
#use strict;
#use warnings;

use Plack::Request;
use Template;
use YAML qw(LoadFile);
use IO::File;

our @LineCache;
sub conll_2_html {
    my ($req, $app) = @_;

	my $file;
	if ( $app->{setup}{file} ) {
		$file = $app->{setup}{file};
	}
	else {
		my $dir = $app->{setup}{directory} || $ENV{PWD};
		$file = join('', $dir, $req->path_info);
	}

	if ( ! -f $file ) {
    	my $res = $req->new_response(404);
    	$res->content_type('text/plain');
    	$res->body('404: Not Found');
    	return $res->finalize;
	}

    my @lines;
    open FILE, '<', $file or die "Can't open $file - $!";
    while (<FILE>) {
        chomp;

        s/\&(?!(#|amp))/\&amp;/g;

        if ( $#LineCache > -1 ) {
            #if ( $_ =~ m/\]/ ) {
            my $pos = index($_, ']');
            if ( $pos > -1 ) {
                # End of a multiline tag
                my $str = substr($_, 0, $pos +1, '');
                my $temp = join '', @LineCache, $str;
                $temp =~ s{\[(PER|ORG|LOC|MISC) (.*?)\]}{<span class='$1'>$2</span>}g;
                #$temp =~ s{\[(PER|ORG|LOC|MISC) (.*?)\]}{<span class='$1'>[$1 $2]</span>}g;
                push @lines, $temp;
                undef @LineCache;
            }
            else {
                # continue a multiline tag
                push @LineCache, $_;
                next;
            }
        }

        if ( m{\[(PER|ORG|LOC|MISC)} ) {
            if ( m{\[(PER|ORG|LOC|MISC) (.*?)\]} ) {
                # Entire tag text is on one line
                s{\[(PER|ORG|LOC|MISC) (.*?)\]}{<span class='$1'>$2</span>}g;
                #s{\[(PER|ORG|LOC|MISC) (.*?)\]}{<span class='$1'>[$1 $2]</span>}g;
            }
            else {
                # Tagged text crosses over several lines
                push @LineCache, $_;
                next;
            }
        }
        push @lines, $_;
    }
    close FILE;
    return \@lines;
}


sub load {
	return LoadFile('app.yml');
}

sub render {
	my $app = shift;
	my $template = \ delete $app->{template};
	my $page;
	my $tt = Template->new({});
	$tt->process($template, $app, \$page) || die $tt->error();
	$page;
}

sub static {
	my ($req, $app) = @_;

	my $file_path = substr $req->path_info(), 1;
	my $fh = new IO::File $file_path, 'r';
	if (defined $fh) {
    	my $res = $req->new_response(200);
    	$res->content_type('text/plain');
    	$res->body($fh);
    	return $res->finalize;
	}
	else {
    	my $res = $req->new_response(404);
    	$res->content_type('text/plain');
    	$res->body('404: Not Found');
    	return $res->finalize;
	}
}

sub index {
	my ($req, $app) = @_;
	$app->{template} = $app->{index_template};
	my $res = $req->new_response(200);
	$res->content_type('text/html');

	opendir DIR, $app->{setup}{directory};
	$app->{listing} = [ sort grep { '.' ne substr $_, 0, 1 } readdir DIR ];
	closedir DIR;

	$res->body(&render($app));
	return $res->finalize;
}


# Init 
{
	my $app = &load;
	if ( ! $app->{setup}{directory} ) {
		print STDERR "Data directory not specified in app.yml\n";
		exit;
	}
	elsif ( ! -d $app->{setup}{directory} )  {
		print STDERR "Specified data directory is not a directory: \"$app->{setup}{directory}\"\n";
		exit;
	}

}

sub {
    my $req = Plack::Request->new(shift);
	my $app = &load;

	return &index($req, $app) if '/' eq $req->path_info;

	for my $url_pattern ( @{$app->{setup}{static}} ) {
		if ($req->path_info =~ m/$url_pattern/) {
			return &static($req, $app);
		}
	}

    my $res = $req->new_response(200);
    $res->content_type('text/html');
	$app->{content} = &conll_2_html($req, $app);
    $res->body(&render($app));
    $res->finalize;
}
