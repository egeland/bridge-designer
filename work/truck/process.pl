use strict;

sub vecs_equal {
    my $a = shift;
    my $b = shift;
    return 0 unless scalar(@$a) == scalar(@$b);
    for (my $i = 0; $i < scalar(@$a); $i++) {
	return 0 if ($a->[$i] != $b->[$i]);
    }
    return 1;
}

sub process_solid {
    my $fn = shift;
    open(IF, "<$fn") or die "Can't open input '$fn'";
    my $line = <IF>;
    die "Missing solid line" unless $line =~ /solid\s+(\S+)/;
    my $solid_name = $1;
    my (@facets);
    while (<IF>) {
	next unless /\S/;
	last if /endsolid/;

	$line = $_; chomp $line;
	die "Missing facet normal in '$line'" unless
	    $line =~ /facet\s+normal\s+(\S+)\s+(\S+)\s+(\S+)/;
	my $normal = [$1 + 0, $2 + 0, $3 + 0];

	$line = <IF>; chomp $line;
	die "Missing outer loop." unless
	    $line =~ /outer loop/;

	my @points;
	foreach my $i (0..2) {
	    $line = <IF>; chomp $line;
	    die "Missing vertex" unless 
		$line =~ /vertex\s+(\S+)\s+(\S+)\s+(\S+)/;
	    push @points, [$1 + 0, $2 + 0, $3 + 0];
	}

	if (@facets && vecs_equal($facets[@facets - 1]->{normal}, $normal)) {
	    push @{$facets[@facets - 1]->{points}}, @points;
	}
	else {
	    push @facets, { normal => $normal, points => \@points };
	}

	$line = <IF>; chomp $line;
	die "Missing endloop." unless
	    $line =~ /endloop/;

	$line = <IF>; chomp $line;
	die "Missing endfacet." unless
	    $line =~ /endfacet/;
    }
    close IF;

    print "    int [] ${solid_name}_triangle_count = {\n";
    foreach my $f (@facets) {
	print "        " . scalar(@{$f->{points}})/3 . ",\n";
    }
    print "    };\n";

    print "    float [] [] ${solid_name}_normals = {\n";
    foreach my $f (@facets) {
	printf "        { %9.5f, %9.5f, %9.5f },\n", 
	       $f->{normal}->[0], $f->{normal}->[1], $f->{normal}->[2];	
    }
    print "    };\n";

    print "    float [] [] ${solid_name}_pts = {\n";
    foreach my $f (@facets) {
	foreach my $p (@{$f->{points}}) {
	    printf "        { %9.5f, %9.5f, %9.5f },\n", $p->[0], $p->[1], $p->[2];	
	}
    }
    print "    };\n";
}

process_solid("tire.stl");
