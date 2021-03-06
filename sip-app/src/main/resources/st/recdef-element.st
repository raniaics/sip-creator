<html>
<table cellpadding="10">
<tr><td><h4>Element $name$</h4></td></tr>
$if (nodeMappings)$
    $nodeMappings : { nm |
        <tr><td><b>&lt;- $nm.inputPath$</b></td></tr>
    }$
$endif$
$if (doc)$
    $if (doc.lines)$
        $doc.lines:{ line |
            <tr><td><p>$line$</p></td></tr>
        }$
    $endif$
    $if (doc.paras)$
        $doc.paras:{ para |
            <tr><td><b>$para.name$</b><br><p>$para.content$</p></td></tr>
        }$
    $endif$
$endif$
$if (optList)$
    <tr><td>
        <ul>
            $optList.opts:{ opt |
                <li>$opt.key$ - $opt.value$</li>
            }$
        </ul>
    </td></tr>
$endif$
$if (node)$
    <tr><td>
        <h1>Dropped here:</h1>
        <h3>$node.path$</h3>
    </td></tr>
$endif$
</table>
</html>