/**
 * Description on how the star icons were generated.
 *
 * @author vtintillier
 */

Those are the SVG sources for the star icons used in this plugin.
star-gold.svg is a copy of the file in /hudson/main/war/images, which itself is
a copy of the Tango project [1] star-gold.svg file.
The other SVG files are derivated from star-gold.svg. Using Inkscape, I modified
the hue, using the ones defined in the Tango project palette
(see /hudson/main/war/images/Tango-Palette.png), using the first column of the
palette.

Those star-*.svg files were then processed by the script that can be found at
/hudson/main/war/images/make.sh. As I did not had any 'svg2png' executable,
as used in the original script, I replaced the line

svg2png -w $sz -h $sz < $src > t.png

by this new one

inkscape -e t.png -w $sz -h $sz $src

I think the quality is a little better than the icons generated in
/hudson/trunk/hudson/main/war/resources/images anyway.

[1] http://tango.freedesktop.org
