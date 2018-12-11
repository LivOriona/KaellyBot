package commands.config;

import commands.model.AbstractCommand;
import data.Constants;
import data.Guild;
import enums.Language;
import exceptions.AdvancedDiscordException;
import exceptions.BasicDiscordException;
import sx.blah.discord.util.DiscordException;
import util.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;
import util.Translator;

import java.util.regex.Matcher;

/**
 * Created by steve on 14/07/2016.
 */
public class PrefixeCommand extends AbstractCommand {

    private final static Logger LOG = LoggerFactory.getLogger(PrefixeCommand.class);
    private exceptions.DiscordException noEnoughRights;
    private exceptions.DiscordException prefixOutOfBounds;

    public PrefixeCommand(){
        super("prefix","\\s+(.+)");
        setUsableInMP(false);
        noEnoughRights = new BasicDiscordException("exception.basic.no_enough_rights");
        prefixOutOfBounds = new AdvancedDiscordException("exception.advanced.prefix_out_of_bound",
                new String[]{String.valueOf(Constants.prefixLimit)}, new Boolean[]{false});
    }

    @Override
    public boolean request(IMessage message) {
        if (super.request(message)) {
            Language lg = Translator.getLanguageFrom(message.getChannel());

            if (isUserHasEnoughRights(message)) {
                Matcher m = getMatcher(message);
                m.find();
                String newPrefix = m.group(1).trim();

                if (newPrefix.length() >= 1 && newPrefix.length() <= Constants.prefixLimit) {
                    Guild.getGuild(message.getGuild()).setPrefix(newPrefix);
                    Message.sendText(message.getChannel(), Translator.getLabel(lg, "prefix.request.1")
                        .replace("{prefix}", getPrefixMdEscaped(message)));
                    try {
                        Message.sendText(message.getGuild().getOwner().getOrCreatePMChannel(),
                                Translator.getLabel(lg, "prefix.request.2")
                                        .replace("{prefix}", getPrefixMdEscaped(message))
                                        .replace("{guild.name}", message.getGuild().getName()));
                    } catch (DiscordException e) {
                        LOG.warn("request", "Impossible de contacter l'administrateur de la guide ["
                                + message.getGuild().getName() + "].");
                    }
                    return true;
                }
                else
                    prefixOutOfBounds.throwException(message, this, lg);
            }
            else
                noEnoughRights.throwException(message, this, lg);
        }
        return false;
    }

    @Override
    public String help(Language lg, String prefix) {
        return "**" + prefix + name + "** " + Translator.getLabel(lg, "prefix.help");
    }

    @Override
    public String helpDetailed(Language lg, String prefix) {
        return help(lg, prefix)
                + "\n" + prefix + "`"  + name + " `*`prefix`* : " + Translator.getLabel(lg, "prefix.help.detailed")
                .replace("{prefixLimit}", String.valueOf(Constants.prefixLimit)) + "\n";
    }
}
