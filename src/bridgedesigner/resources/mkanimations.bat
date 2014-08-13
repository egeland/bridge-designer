@echo off
setlocal
path c:\ImageMagick-6.3.7\bin;%path%
goto new
convert -dispose None -delay 40 -loop 0 ex1pt1fr*.png ex1pt1.gif
convert -dispose None -delay 10 -loop 0 ex1pt2fr*.png ex1pt2.gif
convert -dispose None -delay 40 -loop 0 ex1pt3fr*.png ex1pt3.gif
convert -dispose None -delay 40 -loop 0 ex2pt1fr*.png ex2pt1.gif
convert -dispose None -delay 10 -loop 0 ex2pt2fr*.png ex2pt2.gif
convert -dispose None -delay 40 -loop 0 ex2pt3fr*.png ex2pt3.gif
convert -dispose None                   ex2pt4fr1.png ex2pt4.gif
convert -dispose None -delay 40 -loop 0 ex2pt5fr*.png ex2pt5.gif
convert -dispose None                   ex3pt1fr1.png ex3pt1.gif
convert -dispose None -delay 10 -loop 0 ex3pt2fr*.png ex3pt2.gif
convert -dispose None -delay 40 -loop 0 ex3pt3fr*.png ex3pt3.gif
convert -dispose None -delay 40 -loop 0 ex4pt1fr*.png ex4pt1.gif
convert -dispose None -delay 10 -loop 0 ex4pt2fr*.png ex4pt2.gif
convert -dispose None -delay 40 -loop 0 ex4pt3fr*.png ex4pt3.gif

:new
convert -dispose None -delay 40 -loop 0 ex5pt1fr*.png ex5pt1.gif
convert -dispose None -delay 20 -loop 0 ex5pt2fr*.png ex5pt2.gif
convert -dispose None -delay 80 -loop 0 ex5pt3fr*.png ex5pt3.gif

