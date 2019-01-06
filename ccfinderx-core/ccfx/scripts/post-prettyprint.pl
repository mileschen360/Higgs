#!/bin/perl
# Peter Senna Tschudin - peter.senna@gmail.com
#
# ./post-prettyprint.pl /path/to/ccfinderx_pretty_output /path/to/output.xml
#
# Convert CCFinderX pretty print format to match source code line numbers
# instead of token file line numbers.

use DBI;		# perl-Class-DBI-SQLite.noarch
use XML::Writer;	# perl-XML-Writer.noarch
use Digest::MD5 qw(md5 md5_hex md5_base64);
use strict;

my $DB_FILE= ":memory:";

#SQLite connect
my $dbh = DBI->connect(          
    "dbi:SQLite:dbname=$DB_FILE", 
    "",                          
    "",                          
    { RaiseError => 1 },         
) or die $DBI::errstr;

#Tuning
$dbh->do("PRAGMA synchronous = OFF");
$dbh->do("PRAGMA journal_mode = OFF");
$dbh->do("PRAGMA locking_mode = EXCLUSIVE");
$dbh->do("PRAGMA temp_store = MEMORY");
$dbh->do("PRAGMA PAGE_SIZE = 4096");
$dbh->do("PRAGMA cache_size=2000000");

#Create table
$dbh->do("DROP TABLE IF EXISTS filerefs");
$dbh->do("CREATE TABLE filerefs(id integer primary key autoincrement, cloneid
	integer not null, file text not null, tkn_startln integer not null,
	tkn_endln integer not null, src_startln integer not null, src_endln
	integer not null, md5 text not null)");

print "Reading input file...\n";

#Put the input file in a string
open my $input_file, $ARGV[0] or die "Unable to open file: $ARGV[0]";
my $file_string = join '', <$input_file>;
close $input_file;

#Save contents of source_files { }. The entire block is saved at @source_files[0]
my @source_files = $file_string =~ /source_files\s*( \{ (?: [^{}]* | (?0) )* \} )/xg;
@source_files = split /\n/, $source_files[0]; # Now one line / @array element

#Save contents of clone_pairs { }. The entire block is saved at @clone_pairs[0]
my @clone_pairs = $file_string =~ /clone_pairs\s*( \{ (?: [^{}]* | (?0) )* \} )/xg;
@clone_pairs = split /\n/, $clone_pairs[0]; # Now one line / @array element

#Save file_postfix
(my $file_postfix) = $file_string =~ /option:\s-preprocessed_file_postfix\s(\S*)/xg;

#Creates $source_files_index[$i] with file paths for all file ids
my @source_files_index;
foreach (@source_files){

	my $string = $_;

	#Ignore lines containing { and }
	if ($string =~ /\{|\}/){
		next;
	}

	# 123	/path/to/file	456
	# /(\d+)\t(\S+)\t\d+/ # We want only first two fields
	(my $fileid, my $file) = $string =~ /(\d+)\t(\S+)\t\d+/;

	$source_files_index[$fileid] = $file;
}

print "Populating the DB...\n";

#This is the main loop. Colect all information that will be inserted in the db
my $count = 0;
my $number_clone_pairs = (scalar @clone_pairs) - 1;
foreach (@clone_pairs){
	$count++;

	my $string = $_;

	#Ignore lines containing { and }
	if ($string =~ /\{|\}/){
		next;
	}

        # 40      77.66-145       66.62-141
	# /(\d+)\t(\d+)\.(\d+)\-(\d+)\t(\d+)\.(\d+)\-(\d+)/
	(my $cloneid, my $fileref1, my $tkn_startln1, my $tkn_endln1, my $fileref2,
	my $tkn_startln2, my $tkn_endln2) = $string =~ /(\d+)\t(\d+)\.(\d+)\-(\d+)\t(\d+)\.(\d+)\-(\d+)/;

	# Get src_file numbers instead from tkn_file_numbers. The tln filename
	# ends with the content of $file_postfix
	my $src_startln1 = get_src_ln($source_files_index[$fileref1] .
		$file_postfix, $tkn_startln1);
	my $src_endln1 = get_src_ln($source_files_index[$fileref1] .
		$file_postfix, $tkn_endln1);
	my $src_startln2 = get_src_ln($source_files_index[$fileref2] .
		$file_postfix, $tkn_startln2);;
	my $src_endln2 = get_src_ln($source_files_index[$fileref2] .
		$file_postfix, $tkn_endln2);

	# MD5 is only used for detecting duplicates
	my $md51 = md5_hex("$source_files_index[$fileref1]:$tkn_startln1:$tkn_startln1");
	my $md52 = md5_hex("$source_files_index[$fileref2]:$tkn_startln2:$tkn_startln2");

	# id, cloneid, file, tkn_startln, tkn_endln, src_startln, src_endln, md5
	$dbh->do("INSERT INTO filerefs VALUES(NULL, '$cloneid',
	'$source_files_index[$fileref1]', '$tkn_startln1', '$tkn_endln1',
	'$src_startln1', '$src_endln1', '$md51')");

	# id, cloneid, file, tkn_startln, tkn_endln, src_startln, src_endln, md5
	$dbh->do("INSERT INTO filerefs VALUES(NULL, '$cloneid',
	'$source_files_index[$fileref2]', '$tkn_startln2', '$tkn_endln2',
	'$src_startln2', '$src_endln2', '$md52')");

	print "$count / $number_clone_pairs\r";
}
print "\n";

#Remove duplicates
#
#Foreach cloneid
#   Are there more than one file entry with same md5?
#	Yes -> Delete the first one
#
print "Removing duplicates...\n";

$dbh->do("CREATE INDEX md5_idx on filerefs(md5)");
$dbh->do("CREATE INDEX cloneid_idx on filerefs(cloneid)");
my $count = 0;
my $sth = $dbh->prepare('SELECT DISTINCT cloneid FROM filerefs');
$sth->execute();
while ( my $cloneid = $sth->fetchrow ) {
	my $sth = $dbh->prepare( "SELECT id,md5 FROM filerefs where cloneid =
		'$cloneid'" );
	$sth->execute();
	while ((my $id, my $md5) = $sth->fetchrow()){
			my $sth = $dbh->prepare( "SELECT COUNT(id) FROM filerefs
				WHERE md5 = '$md5' AND cloneid = '$cloneid'");
			$sth->execute();
			if ($sth->fetchrow() > 1){
				my $sth = $dbh->prepare( "DELETE FROM filerefs WHERE id = '$id'" );
				$sth->execute();
				$sth->finish();
			}
			$sth->finish();
	}
	$sth->finish();
	$count++;
	print "$count \r";
}
$sth->finish();
print "\n";

print "Saving the XML file...\n";

#XML Writer stuff
open my $output_file, '>', $ARGV[1] or die "Unable to open file: $ARGV[1]";
my $writer = new XML::Writer ( OUTPUT => $output_file, DATA_MODE => 'true',
	DATA_INDENT => 2 );

$writer->startTag('repository');
my $sth = $dbh->prepare('SELECT DISTINCT cloneid FROM filerefs');
$sth->execute();
while ( my $cloneid = $sth->fetchrow ) {
	$writer->startTag('clone', 'id' => "$cloneid");
	my $sth = $dbh->prepare( "SELECT file,src_startln,src_endln FROM filerefs where
				cloneid = '$cloneid'" );
	$sth->execute();
	while ((my $path, my $src_startln, my $src_endln) = $sth->fetchrow()){
		$writer->startTag('file');
			$writer->startTag('path');
				$writer->characters("$path");
			$writer->endTag();
			$writer->startTag('startline');
				$writer->characters("$src_startln");
			$writer->endTag();
			$writer->startTag('endlineline');
				$writer->characters("$src_endln");
			$writer->endTag();
		$writer->endTag();
	}
	$writer->endTag();
}
$sth->finish();
$writer->endTag();
close $output_file;

# Dump all SQLite.
#my $sth = $dbh->prepare( "SELECT * FROM filerefs" );
#$sth->execute();
#while ( my @row = $sth->fetchrow_array ) {
#	print join ("--", @row) . "\n";
#}

# get_src_ln ($file,$ln_number)
# Open $file and read line $ln_number. Get the hex number between ^ and .,
# convert the number to decimal and return.
#
sub get_src_ln
{
	my $the_line;

	(my $file, my $ln_number) = @_;

	open my $input_file, $file or die "Unable to open file: $file";

	while (<$input_file>) {
		if ($. == $ln_number) {
			$the_line = $_;
			last;
		}
	}

	close $input_file;

	#Get the first hex number found between ^ and .
	($the_line) = $the_line =~ /^([0-9a-f]+)\./i;

	# Convert to decimal and return
	return hex($the_line);
}
