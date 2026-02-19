package com.madhur.news_agent.scheduler;

import com.madhur.news_agent.service.NewsIngestionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class NewsIngestionScheduler {

    private final NewsIngestionService ingestionService;

    public NewsIngestionScheduler(NewsIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    private static int index=0;
    private static final String[] queries = {

            "apple","microsoft","google","openai","meta","amazon","tesla",
            "nvidia","intel","samsung","tiktok","youtube","netflix","spotify",
            "twitter","x",

            "artificial intelligence","generative ai","chatgpt","gemini","llama",
            "deepfake","ai regulation","machine learning",
            "AI AND (policy OR regulation)",
            "data privacy AND AI",

            "stock market","nasdaq","s&p 500","bitcoin","ethereum","crypto scam OR crypto",
            "inflation","trade","interest rates","recession","banking crisis OR bank OR banking",
            "federal reserve","fed","rbi","reserve bank of india","sbi","state bank of india",
            "pnb","punjab national bank","hdfc","bank of baroda",

            "election","parliament","congress","prime minister","president","leader",
            "ukraine","russia","china","taiwan","middle east OR west asia",
            "europe OR northern europe OR eastern europe OR southern europe OR western europe",
            "nordic","baltic","slavic","balkans","scandinavian","dutch",
            "latin america OR south america OR north america OR americas",
            "Caribbeans","africa","antarctica","asia OR east asia OR south asia OR central asia",
            "war","conflict","border","nuclear","nation","UNSC","united nations OR UN",

            "covid","vaccine","who OR world health organization",
            "cancer OR cancer research","space OR space mission",
            "nasa","isro","spacex","cnsa","roscosmos",
            "environment","pollution","climate change","global warming",
            "contamination","medicine","health",

            "cricket","ipl","bcci","football","premier league",
            "champions league","fifa","nba","olympics",

            "hockey","field hockey","ice hockey","NHL","IIHF","FIH",
            "Stanley Cup","Hockey World Cup",
            "Indian hockey team","Hockey India",
            "FIH Pro League","Hockey India League",

            "tennis","ATP","WTA",
            "Australian Open","French Open","Wimbledon","US Open",
            "Novak Djokovic","Carlos Alcaraz","Rafael Nadal",
            "Iga Swiatek","Coco Gauff",

            "religion","extremism","faith","radicalisation OR radicalization",
            "discrimination","racism","hate crime OR hatred","isolation","caste",
            "murder","theft OR robbery","rape OR sexual assault","safety",
            "privacy","cyber crime","fraud OR scam","physical assault",
            "terrorism","immigration","women","men","family","marriage","society",

            "cyber attack","data breach","ransomware","hacking",
            "zero day vulnerability","malware","phishing",
            "cloud security","information leak",

            "ai scam","voice cloning","deepfake video",
            "synthetic media","ai generated content",
            "ai copyright","ai ethics",

            "layoffs","job cuts","hiring freeze",
            "startup funding","venture capital",
            "merger","acquisition","bankruptcy",
            "ipo","market crash","economic slowdown",

            "supreme court","high court",
            "new law","bill passed",
            "court ruling","judgment",
            "legal action","investigation",
            "arrested","charged","sentenced",

            "voting","polling","exit poll",
            "ballot","vote counting",
            "election commission",
            "electoral fraud","election interference",

            "earthquake","flood","cyclone","hurricane",
            "wildfire","landslide","tsunami",
            "drought","heatwave",

            "shooting","stabbing",
            "bomb threat","explosion",
            "accident","train accident",
            "plane crash","road accident",

            "outbreak","epidemic","pandemic",
            "new variant","public health emergency",

            "semiconductor","chip shortage",
            "quantum computing","blockchain",
            "cryptocurrency regulation",
            "web3","metaverse","augmented reality","virtual reality",

            "power outage","blackout",
            "renewable energy","solar power","wind energy",
            "nuclear power","hydrogen energy",

            "carbon emissions","net zero",
            "climate policy","greenhouse gas",
            "deforestation","water scarcity",

            "school exam","board exam",
            "university","college admission",
            "education policy","online education",

            "railway","metro","highway",
            "electric vehicle","ev charging",
            "fuel prices","petrol","diesel",

            "bollywood","hollywood","film industry",
            "movie release","box office collection",
            "film controversy","movie ban",
            "actor","actress","celebrity",

            "web series","ott release",
            "netflix series","amazon prime series",
            "disney plus","hotstar","hbo",

            "music album","song release",
            "concert","world tour",
            "music festival",

            "award show","oscar","academy awards",
            "golden globes","grammys",
            "bafta","cannes film festival",

            "celebrity wedding","celebrity divorce",
            "celebrity breakup",
            "celebrity death","celebrity rumor",
            "celebrity controversy","celebrity scandal",

            "trailer launch","teaser launch",
            "casting announcement",
            "biopic","remake","sequel",

            "reality show","tv show",
            "bigg boss","k-pop","kdrama",

            "youtuber","influencer",
            "social media star",
            "tiktok creator","instagram influencers"

    };

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    //@Scheduled(fixedRate = 60 * 1000)
    public void runDailyIngestion() {

        String today = LocalDate.now().minusDays(2).toString();

        int batchSize = 10;

        for (int i = 0; i < batchSize; i++) {

            try {
                int number = ingestionService.ingest(queries[index], today, "1");
                System.out.println("Inserted " + number + " news articles for: " + queries[index]);
            } catch (Exception e) {
                System.out.println("Skipped for q: " + queries[index] + " because: " + e.getMessage());
            }

            index++;

            if (index >= queries.length) {
                index = 0;
            }
        }
    }

}
