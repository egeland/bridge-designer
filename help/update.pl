use strict;
use File::Find;

our @roots = qw(help.hs default);

sub do_find {
    my $year = shift;
    print STDERR "find $year:\n";
    my $action = sub { 
        return if /.png$/;
        my $hdr = "$File::Find::name:\n";
        open(F, $_) or die "$hdr $!\n";
        my $n = 0;
        while (my $line = <F>) {
            ++$n;
            if (index($line, $year) >= 0) {
                if ($hdr) {
                    print $hdr;
                    $hdr = undef;
                }
                print "$n: $line";
            }
        }
        close F;
    } ;
    find({
        wanted => $action,
        bydepth => 1, 
    }, @roots);
}

sub do_fix {
    my $year = shift;
    my $rpl_year = shift;
    print STDERR "fix $year->$rpl_year:\n";
    my $action = sub {
        return if /.png$/;
        print STDERR "$File::Find::name:\n";
    };
    find({
        wanted => $action,
        bydepth => 1,
    }, @roots);
}

sub main {
    my $mode = "find";
    my $old_year = 2011;
    my $new_year = $old_year + 1;

    if (scalar(@ARGV) > 0) {
        $mode = shift(@ARGV);
    }

    if ($mode eq "find") {
        if (scalar(@ARGV) > 0) {
            $old_year = shift(@ARGV);
            if ($old_year + 0 <= 0) {
                die "Bad year '$old_year'.";
            }
        }
        do_find($old_year);
    }
    elsif ($mode eq "fix") {
        if (scalar(@ARGV) > 0) {
            $old_year = shift(@ARGV);
            if ($old_year + 0 <= 0) {
                die "Bad previous year '$old_year'.\n";
            }
        }
        if (scalar(@ARGV) > 0) {
            $new_year = shift(@ARGV);
            if ($new_year + 0 <= 0) {
                die "Bad new year '$new_year'.";
            }
        }
        else {
            $new_year = $old_year + 1;
        }
        do_fix($old_year, $new_year);
    }
    else {
        die "Unknown mode '$mode'.\n";
    }
}

main;